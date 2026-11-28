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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import free.rm.skytube.businessobjects.FeedFilter;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests that download filters are part of the SQL query, not applied to a loaded list.
 */
public class DownloadedVideosDbTest {

    @AfterEach
    void clearFilters() {
        FeedFilter.getInstance().clear();
    }

    @Test
    void buildsUnfilteredQuery() {
        DownloadedVideosDb.Query query = DownloadedVideosDb.buildDownloadedVideosQuery(null);

        assertTrue(query.sql.contains("FROM " + DownloadedVideosTable.TABLE_NAME + " d"));
        assertTrue(query.sql.contains("ORDER BY d." + DownloadedVideosTable.COL_ORDER + " DESC"));
        assertFalse(query.sql.contains("WHERE"));
        assertArrayEquals(new String[0], query.args);
    }

    @Test
    void appliesFiltersInSql() {
        FeedFilter filter = FeedFilter.getInstance();
        filter.setMaxDurationMinutes(5);
        filter.setTitleQuery("foo");

        DownloadedVideosDb.Query query = DownloadedVideosDb.buildDownloadedVideosQuery(filter);

        assertTrue(query.sql.contains("WHERE ("));
        assertTrue(query.sql.contains("d.duration > 0 AND d.duration < ?"));
        assertTrue(query.sql.contains("LOWER(d.title) LIKE ? ESCAPE '\\'"));
        assertArrayEquals(new String[]{"300", "%foo%"}, query.args);
    }
}
