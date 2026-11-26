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

import com.github.skytube.components.utils.SQLiteHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import free.rm.skytube.businessobjects.Logger;

/**
 * Copies the contents of the old, separate SQLite files into the one, shared {@code subs.db}.
 *
 * <p>The legacy files are opened as separate, read-only databases while {@code subs.db} is being
 * upgraded. Rows are copied with {@link ContentValues}, which preserves the BLOBs storing
 * serialized videos exactly as they are and keeps every write inside the upgrade transaction.
 * Only columns which exist both in the old and in the new schema are copied, so databases from
 * older versions (e.g. without SponsorBlock or search dates) are imported as well.</p>
 */
final class LegacyDatabaseMigration {
    private static final String BOOKMARKS_DB = "bookmarks.db";
    private static final String DOWNLOADS_DB = "videodownloads.db";
    private static final String PLAYBACK_DB = "playbackhistory.db";
    private static final String CHANNEL_FILTERING_DB = "channelFiltering.db";
    private static final String SEARCH_HISTORY_DB = "searchHistory.db";

    private static final LegacyTable[] LEGACY_TABLES = {
            new LegacyTable(BOOKMARKS_DB, BookmarksTable.TABLE_NAME,
                    BookmarksTable.COL_YOUTUBE_VIDEO_ID, BookmarksTable.COL_YOUTUBE_VIDEO,
                    BookmarksTable.COL_ORDER),
            new LegacyTable(DOWNLOADS_DB, DownloadedVideosTable.TABLE_NAME,
                    DownloadedVideosTable.COL_YOUTUBE_VIDEO_ID, DownloadedVideosTable.COL_YOUTUBE_VIDEO,
                    DownloadedVideosTable.COL_FILE_URI, DownloadedVideosTable.COL_AUDIO_FILE_URI,
                    DownloadedVideosTable.COL_SB, DownloadedVideosTable.COL_ORDER),
            new LegacyTable(PLAYBACK_DB, PlaybackStatusTable.TABLE_NAME,
                    PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID,
                    PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION,
                    PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED),
            new LegacyTable(CHANNEL_FILTERING_DB, ChannelListTable.BLACKLIST.getTableName(),
                    ChannelListTable.COL_ID, ChannelListTable.COL_CHANNEL_ID,
                    ChannelListTable.COL_CHANNEL_NAME),
            new LegacyTable(CHANNEL_FILTERING_DB, ChannelListTable.WHITELIST.getTableName(),
                    ChannelListTable.COL_ID, ChannelListTable.COL_CHANNEL_ID,
                    ChannelListTable.COL_CHANNEL_NAME),
            new LegacyTable(SEARCH_HISTORY_DB, SearchHistoryTable.TABLE_NAME,
                    SearchHistoryTable.COL_SEARCH_ID, SearchHistoryTable.COL_SEARCH_TEXT,
                    SearchHistoryTable.COL_SEARCH_DATE),
    };

    private LegacyDatabaseMigration() {
    }

    /**
     * Create all the tables which used to live in a separate database. Creating a table which is
     * already there is harmless: that happens if a backup with a partially upgraded database is
     * restored.
     */
    static void createConsolidatedTables(SQLiteDatabase db) {
        SQLiteHelper.continueOnError(db, BookmarksTable.getCreateStatement());
        SQLiteHelper.continueOnError(db, DownloadedVideosTable.getCreateStatement());
        SQLiteHelper.continueOnError(db, PlaybackStatusTable.getCreateStatement());
        SQLiteHelper.continueOnError(db, SearchHistoryTable.getCreateStatement());
        SQLiteHelper.continueOnError(db, ChannelListTable.BLACKLIST.getCreateStatement());
        SQLiteHelper.continueOnError(db, ChannelListTable.WHITELIST.getCreateStatement());
    }

    /**
     * Import every legacy database found in {@code databaseDirectory}. Files which cannot be read
     * are left untouched, to avoid losing data.
     *
     * @return The files whose contents were successfully imported. They must only be deleted after
     * the upgrade transaction has been committed; see {@link #deleteImportedFiles(List)}.
     */
    static List<File> importLegacyDatabases(SQLiteDatabase db, File databaseDirectory) {
        createConsolidatedTables(db);
        if (databaseDirectory == null) {
            return new ArrayList<>();
        }
        // A legacy file can hold more than one table (channelFiltering.db), so a file is only
        // reported once all its tables have been imported.
        final Set<File> importedFiles = new LinkedHashSet<>();
        for (LegacyTable table : LEGACY_TABLES) {
            final File legacyFile = new File(databaseDirectory, table.databaseFile);
            if (!legacyFile.isFile()) {
                continue;
            }
            if (importTable(db, legacyFile, table)) {
                importedFiles.add(legacyFile);
            }
        }
        return new ArrayList<>(importedFiles);
    }

    /**
     * Remove the legacy files (and their journal files) after their data has been committed to
     * {@code subs.db}.
     */
    static void deleteImportedFiles(List<File> files) {
        for (File file : files) {
            deleteDatabaseFile(file);
        }
    }

    private static boolean importTable(SQLiteDatabase target, File legacyFile, LegacyTable table) {
        SQLiteDatabase source = null;
        try {
            source = openLegacyDatabase(legacyFile);
            final List<String> columns = commonColumns(source, table);
            if (columns.isEmpty()) {
                Logger.w(LegacyDatabaseMigration.class, "No %s table found in %s", table.sourceTable, legacyFile);
                return false;
            }
            int imported = 0;
            try (Cursor cursor = source.rawQuery("SELECT " + join(columns) + " FROM " + table.sourceTable, null)) {
                while (cursor.moveToNext()) {
                    final ContentValues values = new ContentValues();
                    for (int i = 0; i < columns.size(); i++) {
                        putValue(values, columns.get(i), cursor, i);
                    }
                    target.insertWithOnConflict(table.targetTable, null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    imported++;
                }
            }
            Logger.i(LegacyDatabaseMigration.class, "Imported %d rows from %s into %s", imported, legacyFile, table.targetTable);
            return true;
        } catch (SQLException | IllegalStateException e) {
            Logger.e(LegacyDatabaseMigration.class, e, "Unable to import %s from %s", table.sourceTable, legacyFile);
            return false;
        } finally {
            if (source != null) {
                source.close();
            }
        }
    }

    /**
     * Reading an old database does not need write access, but a leftover rollback journal may
     * require it. Try the safer, read-only mode first and fall back to read/write in that case.
     */
    private static SQLiteDatabase openLegacyDatabase(File legacyFile) {
        try {
            return SQLiteDatabase.openDatabase(legacyFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        } catch (SQLException e) {
            Logger.w(LegacyDatabaseMigration.class, "Retrying to read %s with write access", legacyFile);
            return SQLiteDatabase.openDatabase(legacyFile.getPath(), null, SQLiteDatabase.OPEN_READWRITE);
        }
    }

    /**
     * @return The target columns which are also present in the legacy table, in target order.
     */
    private static List<String> commonColumns(SQLiteDatabase source, LegacyTable table) {
        final List<String> result = new ArrayList<>();
        try (Cursor cursor = source.rawQuery("SELECT * FROM " + table.sourceTable + " LIMIT 0", null)) {
            if (cursor == null) {
                return result;
            }
            final Set<String> available = new HashSet<>(Arrays.asList(cursor.getColumnNames()));
            for (String column : table.targetColumns) {
                if (available.contains(column)) {
                    result.add(column);
                }
            }
        }
        return result;
    }

    private static void putValue(ContentValues values, String column, Cursor cursor, int index) {
        switch (cursor.getType(index)) {
            case Cursor.FIELD_TYPE_NULL:
                values.putNull(column);
                break;
            case Cursor.FIELD_TYPE_INTEGER:
                values.put(column, cursor.getLong(index));
                break;
            case Cursor.FIELD_TYPE_FLOAT:
                values.put(column, cursor.getDouble(index));
                break;
            case Cursor.FIELD_TYPE_BLOB:
                values.put(column, cursor.getBlob(index));
                break;
            default:
                values.put(column, cursor.getString(index));
                break;
        }
    }

    private static String join(List<String> columns) {
        final StringBuilder builder = new StringBuilder();
        for (String column : columns) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(column);
        }
        return builder.toString();
    }

    private static void deleteDatabaseFile(File file) {
        for (String suffix : new String[]{"", "-journal", "-wal", "-shm"}) {
            File fileToDelete = new File(file.getPath() + suffix);
            if (fileToDelete.exists() && !fileToDelete.delete()) {
                Logger.w(LegacyDatabaseMigration.class, "Unable to delete old database file %s", fileToDelete);
            }
        }
    }

    private static final class LegacyTable {
        final String databaseFile;
        final String sourceTable;
        final String targetTable;
        final String[] targetColumns;

        LegacyTable(String databaseFile, String tableName, String... targetColumns) {
            this.databaseFile = databaseFile;
            this.sourceTable = tableName;
            this.targetTable = tableName;
            this.targetColumns = targetColumns;
        }
    }
}
