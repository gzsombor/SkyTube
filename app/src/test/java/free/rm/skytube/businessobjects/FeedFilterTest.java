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
package free.rm.skytube.businessobjects;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class FeedFilterTest {
    private static final long CUTOFF = 1_700_000_000_000L;

    private final FeedFilter filter = FeedFilter.getInstance();

    @BeforeEach
    @AfterEach
    void clearFilter() {
        filter.clear();
    }

    @Test
    void inactiveFilterBuildsNoDatabaseWhereClause() {
        List<String> args = new ArrayList<>();
        assertEquals("", filter.buildSqlWhere("s", args));
        assertTrue(args.isEmpty());
        assertFalse(filter.isActive());
    }

    @Test
    void blankTitleQueryDoesNotActivateFilter() {
        filter.setTitleQuery("  ");
        assertFalse(filter.isActive());

        List<String> args = new ArrayList<>();
        assertEquals("", filter.buildSqlWhere("s", args));
        assertTrue(args.isEmpty());
    }

    @Test
    void buildsDatabaseWhereClauseForAllActiveFilters() {
        filter.setBeforeTimestamp(CUTOFF);
        filter.setMaxDurationMinutes(5);
        filter.setMinDurationMinutes(15);
        filter.setTitleQuery(" C_t ");
        filter.setUnwatchedOnly(true);
        filter.setPartiallyWatchedOnly(true);

        List<String> args = new ArrayList<>();
        String where = filter.buildSqlWhere("s", args);

        assertTrue(where.contains("s.publish_time > 0 AND s.publish_time < ?"));
        assertTrue(where.contains("s.duration > 0 AND s.duration < ?"));
        assertTrue(where.contains("s.duration > ?"));
        assertTrue(where.contains("LOWER(s.title) LIKE ? ESCAPE '\\'"));
        assertTrue(where.contains("NOT EXISTS (SELECT 1 FROM PlaybackHistory p"));
        assertEquals(Arrays.asList(String.valueOf(CUTOFF), "300", "900", "%c\\_t%"), args);
    }

    @Test
    void buildsUnwatchedAndPartiallyWatchedDatabaseClauses() {
        filter.setUnwatchedOnly(true);
        List<String> args = new ArrayList<>();
        String where = filter.buildSqlWhere("s", args);
        assertTrue(where.startsWith("NOT EXISTS"));
        assertTrue(where.contains("p.YouTube_Video_Position > 0 OR p.YouTube_Video_Watched = 1"));
        assertTrue(args.isEmpty());

        filter.clear();
        filter.setPartiallyWatchedOnly(true);
        args = new ArrayList<>();
        where = filter.buildSqlWhere("s", args);
        assertTrue(where.startsWith("EXISTS"));
        assertTrue(where.contains("COALESCE(p.YouTube_Video_Watched,0) = 0"));
        assertTrue(args.isEmpty());

        filter.setUnwatchedOnly(true);
        args = new ArrayList<>();
        where = filter.buildSqlWhere("s", args);
        assertTrue(where.startsWith("NOT EXISTS"));
        assertTrue(where.contains("COALESCE(p.YouTube_Video_Watched,0) = 1"));
        assertTrue(args.isEmpty());
    }

    @Test
    void clearResetsAllFilters() {
        filter.setBeforeTimestamp(CUTOFF);
        filter.setMaxDurationMinutes(5);
        filter.setMinDurationMinutes(15);
        filter.setTitleQuery("cats");
        filter.setUnwatchedOnly(true);
        filter.setPartiallyWatchedOnly(true);
        assertTrue(filter.isActive());

        filter.clear();
        assertFalse(filter.isActive());
        List<String> args = new ArrayList<>();
        assertEquals("", filter.buildSqlWhere("s", args));
        assertTrue(args.isEmpty());
    }
}
