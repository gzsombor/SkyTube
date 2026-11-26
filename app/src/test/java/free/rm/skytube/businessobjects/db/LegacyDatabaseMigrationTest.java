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
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for importing the contents of the old, separate database files into subs.db.
 */
public class LegacyDatabaseMigrationTest {

    @TempDir
    File databaseDirectory;

    private static final String[] ALL_COLUMNS = {
            BookmarksTable.COL_YOUTUBE_VIDEO_ID, BookmarksTable.COL_YOUTUBE_VIDEO,
            BookmarksTable.COL_ORDER, DownloadedVideosTable.COL_FILE_URI,
            DownloadedVideosTable.COL_AUDIO_FILE_URI, DownloadedVideosTable.COL_SB,
            PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION,
            PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED,
            ChannelListTable.COL_ID, ChannelListTable.COL_CHANNEL_ID,
            ChannelListTable.COL_CHANNEL_NAME, SearchHistoryTable.COL_SEARCH_TEXT,
            SearchHistoryTable.COL_SEARCH_DATE,
    };

    private static final String[] LEGACY_FILES = {
            "bookmarks.db", "videodownloads.db", "playbackhistory.db",
            "channelFiltering.db", "searchHistory.db",
    };

    @Test
    void createsEveryFormerDatabaseTable() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);

        LegacyDatabaseMigration.createConsolidatedTables(db);

        verify(db).execSQL(BookmarksTable.getCreateStatement());
        verify(db).execSQL(DownloadedVideosTable.getCreateStatement());
        verify(db).execSQL(PlaybackStatusTable.getCreateStatement());
        verify(db).execSQL(SearchHistoryTable.getCreateStatement());
        verify(db).execSQL(ChannelListTable.BLACKLIST.getCreateStatement());
        verify(db).execSQL(ChannelListTable.WHITELIST.getCreateStatement());
    }

    @Test
    void importsAllLegacyTablesAndDeletesTheOldFilesOnlyWhenAsked() throws IOException {
        for (String name : LEGACY_FILES) {
            legacyFile(name);
        }
        SQLiteDatabase target = mock(SQLiteDatabase.class);
        SQLiteDatabase source = legacySource(ALL_COLUMNS);
        final List<File> importedFiles;
        try (MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class);
             MockedConstruction<ContentValues> values = mockConstruction(ContentValues.class)) {
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), isNull(), anyInt())).thenReturn(source);

            importedFiles = LegacyDatabaseMigration.importLegacyDatabases(target, databaseDirectory);

            for (String table : new String[]{BookmarksTable.TABLE_NAME, DownloadedVideosTable.TABLE_NAME,
                    PlaybackStatusTable.TABLE_NAME, ChannelListTable.BLACKLIST.getTableName(),
                    ChannelListTable.WHITELIST.getTableName(), SearchHistoryTable.TABLE_NAME}) {
                verify(target, atLeastOnce()).insertWithOnConflict(eq(table), isNull(),
                        any(ContentValues.class), eq(SQLiteDatabase.CONFLICT_IGNORE));
            }
            assertTrue(values.constructed().size() >= LEGACY_FILES.length,
                    "Every legacy row should produce a set of values");
        }
        assertEquals(LEGACY_FILES.length, importedFiles.size());
        for (String name : LEGACY_FILES) {
            assertTrue(new File(databaseDirectory, name).exists(),
                    "The upgrade transaction has not committed yet: " + name);
        }

        LegacyDatabaseMigration.deleteImportedFiles(importedFiles);

        for (String name : LEGACY_FILES) {
            assertFalse(new File(databaseDirectory, name).exists(), "Legacy file was not removed: " + name);
        }
    }

    @Test
    void copiesOnlyTheColumnsWhichExistInTheLegacySchema() throws IOException {
        legacyFile("searchHistory.db");
        SQLiteDatabase target = mock(SQLiteDatabase.class);
        // An old search history database has no Search_Date column.
        SQLiteDatabase source = legacySource(SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT);

        try (MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class)) {
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), isNull(), anyInt())).thenReturn(source);

            LegacyDatabaseMigration.importLegacyDatabases(target, databaseDirectory);
        }

        verify(source).rawQuery("SELECT " + SearchHistoryTable.COL_SEARCH_ID + ", "
                + SearchHistoryTable.COL_SEARCH_TEXT + " FROM " + SearchHistoryTable.TABLE_NAME, null);
        verify(target).insertWithOnConflict(eq(SearchHistoryTable.TABLE_NAME), isNull(),
                any(ContentValues.class), eq(SQLiteDatabase.CONFLICT_IGNORE));
    }

    @Test
    void missingLegacyFileIsIgnored() {
        SQLiteDatabase target = mock(SQLiteDatabase.class);

        try (MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class)) {
            LegacyDatabaseMigration.importLegacyDatabases(target, databaseDirectory);

            open.verify(() -> SQLiteDatabase.openDatabase(anyString(), isNull(), anyInt()), never());
        }
        verify(target, never()).insertWithOnConflict(anyString(), isNull(),
                any(ContentValues.class), anyInt());
    }

    @Test
    void legacyFileWhichCannotBeReadIsKept() throws IOException {
        legacyFile("bookmarks.db");
        legacyFile("videodownloads.db");
        SQLiteDatabase target = mock(SQLiteDatabase.class);
        SQLiteDatabase source = legacySource(ALL_COLUMNS);
        SQLException failure = mock(SQLException.class);

        final List<File> importedFiles;
        try (MockedStatic<SQLiteDatabase> open = mockStatic(SQLiteDatabase.class)) {
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), isNull(), eq(SQLiteDatabase.OPEN_READONLY)))
                    .thenAnswer(invocation -> {
                        if (invocation.<String>getArgument(0).contains("videodownloads.db")) {
                            throw failure;
                        }
                        return source;
                    });
            open.when(() -> SQLiteDatabase.openDatabase(anyString(), isNull(), eq(SQLiteDatabase.OPEN_READWRITE)))
                    .thenThrow(failure);

            importedFiles = LegacyDatabaseMigration.importLegacyDatabases(target, databaseDirectory);
        }

        assertEquals(1, importedFiles.size(), "Only the readable database should be reported");
        LegacyDatabaseMigration.deleteImportedFiles(importedFiles);

        assertTrue(new File(databaseDirectory, "videodownloads.db").exists(),
                "A database which could not be read must not be deleted");
        assertFalse(new File(databaseDirectory, "bookmarks.db").exists(),
                "The other databases should still be imported");
    }

    private void legacyFile(String name) throws IOException {
        Files.write(new File(databaseDirectory, name).toPath(), new byte[]{1, 2, 3});
    }

    /**
     * A legacy database whose tables have the given columns and contain a single row.
     */
    private static SQLiteDatabase legacySource(String... columns) {
        SQLiteDatabase source = mock(SQLiteDatabase.class);
        when(source.rawQuery(anyString(), isNull())).thenAnswer(invocation -> {
            String query = invocation.getArgument(0);
            Cursor cursor = mock(Cursor.class);
            when(cursor.getColumnNames()).thenReturn(columns);
            if (query.startsWith("SELECT *")) {
                when(cursor.moveToNext()).thenReturn(false);
            } else {
                when(cursor.moveToNext()).thenReturn(true, false);
                when(cursor.getType(anyInt())).thenReturn(Cursor.FIELD_TYPE_STRING);
                when(cursor.getString(anyInt())).thenAnswer(value -> "value-" + value.getArgument(0));
            }
            return cursor;
        });
        return source;
    }
}
