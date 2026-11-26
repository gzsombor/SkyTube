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
package free.rm.skytube.gui.businessobjects.preferences;

import android.content.SharedPreferences;
import android.os.Environment;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collections;
import java.util.Enumeration;
import java.util.zip.ZipEntry;

import free.rm.skytube.app.SkyTubeApp;
import free.rm.skytube.businessobjects.db.BookmarksDb;
import free.rm.skytube.businessobjects.db.ChannelFilteringDb;
import free.rm.skytube.businessobjects.db.PlaybackStatusDb;
import free.rm.skytube.businessobjects.db.SearchHistoryDb;
import free.rm.skytube.businessobjects.db.SubscriptionsDb;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Tests for the new (single database) and the old (multiple databases) backup formats.
 */
public class DatabaseBackupFormatTest {

    @TempDir
    File temporaryDirectory;

    @Test
    void newBackupContainsOnlyTheConsolidatedDatabase() throws IOException {
        File databaseFile = new File(temporaryDirectory, "subs.db");
        Files.write(databaseFile.toPath(), new byte[]{1, 2, 3});
        SubscriptionsDb subscriptionsDb = mock(SubscriptionsDb.class);
        when(subscriptionsDb.getDatabasePath()).thenReturn(databaseFile.getPath());
        SharedPreferences preferences = mock(SharedPreferences.class);
        when(preferences.getAll()).thenReturn(Collections.emptyMap());

        String backupPath;
        try (MockedStatic<Environment> environment = mockStatic(Environment.class);
             MockedStatic<SkyTubeApp> app = mockStatic(SkyTubeApp.class);
             MockedStatic<SubscriptionsDb> subscriptions = mockStatic(SubscriptionsDb.class);
             MockedStatic<BookmarksDb> bookmarks = mockStatic(BookmarksDb.class);
             MockedStatic<PlaybackStatusDb> playback = mockStatic(PlaybackStatusDb.class);
             MockedStatic<ChannelFilteringDb> filtering = mockStatic(ChannelFilteringDb.class);
             MockedStatic<SearchHistoryDb> search = mockStatic(SearchHistoryDb.class)) {
            environment.when(Environment::getExternalStorageDirectory).thenReturn(temporaryDirectory);
            app.when(SkyTubeApp::getPreferenceManager).thenReturn(preferences);
            app.when(() -> SkyTubeApp.getStr(anyInt())).thenReturn("pref");
            subscriptions.when(SubscriptionsDb::getSubscriptionsDb).thenReturn(subscriptionsDb);
            bookmarks.when(BookmarksDb::getBookmarksDb).thenReturn(mock(BookmarksDb.class));
            playback.when(PlaybackStatusDb::getPlaybackStatusDb).thenReturn(mock(PlaybackStatusDb.class));
            filtering.when(ChannelFilteringDb::getChannelFilteringDb).thenReturn(mock(ChannelFilteringDb.class));
            search.when(SearchHistoryDb::getSearchHistoryDb).thenReturn(mock(SearchHistoryDb.class));

            backupPath = new BackupDatabases().backupDbsToSdCard();
        }

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(backupPath)) {
            assertTrue(hasEntry(zip, "subs.db"));
            assertTrue(hasEntry(zip, BackupDatabases.PREFERENCES_JSON));
            for (String oldDatabase : new String[]{"bookmarks.db", "videodownloads.db",
                    "playbackhistory.db", "channelFiltering.db", "searchHistory.db"}) {
                assertFalse(hasEntry(zip, oldDatabase), "Old database should not be exported: " + oldDatabase);
            }
        }
    }

    @Test
    void oldBackupFormatCanStillBeUnzippedIntoTheDatabaseDirectory() throws IOException {
        File sourceDirectory = new File(temporaryDirectory, "old");
        File databaseDirectory = new File(temporaryDirectory, "databases");
        assertTrue(sourceDirectory.mkdirs());
        assertTrue(databaseDirectory.mkdirs());
        byte[] subs = new byte[]{1, 2, 3};
        byte[] bookmarks = new byte[]{4, 5, 6};
        File subsDb = new File(sourceDirectory, "subs.db");
        File bookmarksDb = new File(sourceDirectory, "bookmarks.db");
        Files.write(subsDb.toPath(), subs);
        Files.write(bookmarksDb.toPath(), bookmarks);

        File archive = new File(temporaryDirectory, "old.skytube");
        try (ZipOutput output = new ZipOutput(archive)) {
            output.addFile(subsDb.getPath());
            output.addFile(bookmarksDb.getPath());
            output.addContent(BackupDatabases.PREFERENCES_JSON, "{}");
        }

        java.util.Map<String, ZipFile.JsonFile> result = new ZipFile(archive).unzip(databaseDirectory);

        assertArrayEquals(subs, Files.readAllBytes(new File(databaseDirectory, "subs.db").toPath()));
        assertArrayEquals(bookmarks, Files.readAllBytes(new File(databaseDirectory, "bookmarks.db").toPath()));
        assertTrue(result.containsKey(BackupDatabases.PREFERENCES_JSON));
        assertEquals("{}", result.get(BackupDatabases.PREFERENCES_JSON).content);
    }

    private static boolean hasEntry(java.util.zip.ZipFile zip, String name) {
        Enumeration<? extends ZipEntry> entries = zip.entries();
        while (entries.hasMoreElements()) {
            if (name.equals(entries.nextElement().getName())) {
                return true;
            }
        }
        return false;
    }
}
