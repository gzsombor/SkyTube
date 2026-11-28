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

import android.database.sqlite.SQLiteDatabase;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import free.rm.skytube.businessobjects.FeedFilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests that bookmark filters are part of the SQL query, before the page limit.
 */
public class BookmarksDbTest {

    @AfterEach
    void clearFilters() {
        FeedFilter.getInstance().clear();
    }

    @Test
    void buildsUnfilteredPagedQuery() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);

        BookmarksDb.createBookmarkQuery(20, 15, db, FeedFilter.getInstance());

        CapturedQuery query = capture(db);
        assertTrue(query.sql.contains("b." + BookmarksTable.COL_ORDER + " < ?"));
        assertTrue(query.sql.contains("ORDER BY b." + BookmarksTable.COL_ORDER + " DESC LIMIT ?"));
        assertArrayEquals(new String[]{"15", "20"}, query.args);
    }

    @Test
    void appliesFiltersBeforeThePageLimit() {
        SQLiteDatabase db = mock(SQLiteDatabase.class);
        FeedFilter filter = FeedFilter.getInstance();
        filter.setMinDurationMinutes(1);
        filter.setTitleQuery("foo");

        BookmarksDb.createBookmarkQuery(20, 15, db, filter);

        CapturedQuery query = capture(db);
        assertTrue(query.sql.indexOf("WHERE") < query.sql.indexOf("LIMIT"));
        assertTrue(query.sql.contains("b.duration > ?"));
        assertTrue(query.sql.contains("LOWER(b.title) LIKE ? ESCAPE '\\'"));
        assertArrayEquals(new String[]{"15", "60", "%foo%", "20"}, query.args);
    }

    private static CapturedQuery capture(SQLiteDatabase db) {
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        //noinspection unchecked
        ArgumentCaptor<String[]> args = ArgumentCaptor.forClass(String[].class);
        verify(db).rawQuery(sql.capture(), args.capture());
        return new CapturedQuery(sql.getValue(), args.getValue());
    }

    private static final class CapturedQuery {
        private final String sql;
        private final String[] args;

        private CapturedQuery(String sql, String[] args) {
            this.sql = sql;
            this.args = args;
        }
    }
}
