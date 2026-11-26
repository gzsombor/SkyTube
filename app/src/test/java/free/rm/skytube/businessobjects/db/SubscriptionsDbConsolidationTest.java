/*
 * SkyTube
 * Copyright (C) 2026  SkyTube contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation (version 3 of the License).
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package free.rm.skytube.businessobjects.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import free.rm.skytube.app.SkyTubeApp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Tests that the single database owns every table and that upgrading it imports legacy data.
 */
public class SubscriptionsDbConsolidationTest {

    @TempDir
    File databaseDirectory;

    @Test
    void onCreateCreatesTheTablesOfAllFormerDatabases() {
        SQLiteDatabase sqlite = mock(SQLiteDatabase.class);
        when(sqlite.rawQuery(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(mock(Cursor.class));
        SubscriptionsDb db = mock(SubscriptionsDb.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        org.mockito.Mockito.doReturn(databaseDirectory).when(db).getLegacyDatabasesDirectory();

        Context context = mock(Context.class);
        when(context.getResources()).thenReturn(mock(Resources.class));
        try (MockedStatic<SkyTubeApp> app = mockStatic(SkyTubeApp.class)) {
            app.when(SkyTubeApp::getContext).thenReturn(context);
            app.when(() -> SkyTubeApp.getStr(anyInt())).thenReturn("pref");

            db.onCreate(sqlite);
        }

        List<String> statements = executedStatements(sqlite);
        for (String table : new String[]{SubscriptionsTable.TABLE_NAME,
                SubscriptionsVideosTable.TABLE_NAME_V2, LocalChannelTable.TABLE_NAME,
                CategoriesTable.TABLE_NAME, BookmarksTable.TABLE_NAME,
                DownloadedVideosTable.TABLE_NAME, PlaybackStatusTable.TABLE_NAME,
                SearchHistoryTable.TABLE_NAME, ChannelListTable.BLACKLIST.getTableName(),
                ChannelListTable.WHITELIST.getTableName()}) {
            assertTrue(has(statements, "CREATE TABLE " + table), "Missing table " + table + " in " + statements);
        }
    }

    @Test
    void upgradeTo20ImportsLegacyDatabases() throws IOException {
        Files.write(new File(databaseDirectory, "bookmarks.db").toPath(), new byte[]{1});
        SQLiteDatabase sqlite = mock(SQLiteDatabase.class);
        SQLiteDatabase legacyDb = legacyBookmarksSource();
        SubscriptionsDb db = mock(SubscriptionsDb.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        org.mockito.Mockito.doReturn(databaseDirectory).when(db).getLegacyDatabasesDirectory();

        try (MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class)) {
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), org.mockito.ArgumentMatchers.isNull(), anyInt()))
                    .thenReturn(legacyDb);

            db.onUpgrade(sqlite, 19, 20);
        }

        verify(sqlite).insertWithOnConflict(eq(BookmarksTable.TABLE_NAME),
                org.mockito.ArgumentMatchers.isNull(), any(ContentValues.class),
                eq(SQLiteDatabase.CONFLICT_IGNORE));
        File legacyFile = new File(databaseDirectory, "bookmarks.db");
        assertTrue(legacyFile.exists(), "The legacy file must survive until the upgrade is committed");

        db.onOpen(sqlite);

        assertFalse(legacyFile.exists(), "onOpen runs after the transaction and cleans up the legacy file");
    }

    @Test
    void createDatabaseImportsLegacyFilesToo() throws IOException {
        Files.write(new File(databaseDirectory, "bookmarks.db").toPath(), new byte[]{1});
        SQLiteDatabase sqlite = mock(SQLiteDatabase.class);
        when(sqlite.rawQuery(anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(mock(Cursor.class));
        SQLiteDatabase legacyDb = legacyBookmarksSource();
        SubscriptionsDb db = mock(SubscriptionsDb.class, withSettings().defaultAnswer(CALLS_REAL_METHODS));
        org.mockito.Mockito.doReturn(databaseDirectory).when(db).getLegacyDatabasesDirectory();

        Context context = mock(Context.class);
        when(context.getResources()).thenReturn(mock(Resources.class));
        try (MockedStatic<SkyTubeApp> app = mockStatic(SkyTubeApp.class);
             MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class)) {
            app.when(SkyTubeApp::getContext).thenReturn(context);
            app.when(() -> SkyTubeApp.getStr(anyInt())).thenReturn("pref");
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), org.mockito.ArgumentMatchers.isNull(), anyInt()))
                    .thenReturn(legacyDb);

            db.onCreate(sqlite);

            db.onOpen(sqlite);
        }

        verify(sqlite).insertWithOnConflict(eq(BookmarksTable.TABLE_NAME),
                org.mockito.ArgumentMatchers.isNull(), any(ContentValues.class),
                eq(SQLiteDatabase.CONFLICT_IGNORE));
        assertFalse(new File(databaseDirectory, "bookmarks.db").exists(),
                "Even a fresh subs.db must not leave legacy data behind");
    }

    /**
     * A legacy bookmarks database containing one row.
     */
    private static SQLiteDatabase legacyBookmarksSource() {
        SQLiteDatabase legacyDb = mock(SQLiteDatabase.class);
        when(legacyDb.rawQuery(anyString(), org.mockito.ArgumentMatchers.isNull())).thenAnswer(invocation -> {
            Cursor cursor = mock(Cursor.class);
            String query = invocation.getArgument(0);
            if (query.startsWith("SELECT *")) {
                when(cursor.getColumnNames()).thenReturn(new String[]{BookmarksTable.COL_YOUTUBE_VIDEO_ID,
                        BookmarksTable.COL_YOUTUBE_VIDEO, BookmarksTable.COL_ORDER});
                when(cursor.moveToNext()).thenReturn(false);
            } else {
                when(cursor.moveToNext()).thenReturn(true, false);
                when(cursor.getType(anyInt())).thenReturn(Cursor.FIELD_TYPE_STRING);
                when(cursor.getString(anyInt())).thenReturn("value");
            }
            return cursor;
        });
        return legacyDb;
    }

    private static List<String> executedStatements(SQLiteDatabase db) {
        ArgumentCaptor<String> statements = ArgumentCaptor.forClass(String.class);
        verify(db, atLeastOnce()).execSQL(statements.capture());
        return statements.getAllValues();
    }

    private static boolean has(List<String> statements, String... parts) {
        for (String statement : statements) {
            boolean matches = true;
            for (String part : parts) {
                matches &= statement.contains(part);
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }
}
