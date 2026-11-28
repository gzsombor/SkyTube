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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import free.rm.skytube.businessobjects.db.PlaybackStatusTable;
import free.rm.skytube.businessobjects.db.SubscriptionsVideosTable;

/**
 * Holds the (session-only) feed filter values selected in the right-side filter drawer and turns
 * them into SQL conditions which the database applies before pagination.
 *
 * <p>Every active filter narrows the feed: a video has to satisfy all of the active criteria.  The
 * two playback-status filters are the exception - when both are enabled a video is kept if it is
 * either unwatched or only partially watched.</p>
 */
public final class FeedFilter {
    private static final FeedFilter INSTANCE = new FeedFilter();

    private Long beforeTimestamp = null;
    private Integer maxDurationMinutes = null;
    private Integer minDurationMinutes = null;
    private boolean unwatchedOnly = false;
    private boolean partiallyWatchedOnly = false;
    private String titleQuery = "";

    private FeedFilter() {
    }

    public static FeedFilter getInstance() {
        return INSTANCE;
    }

    public synchronized void clear() {
        beforeTimestamp = null;
        maxDurationMinutes = null;
        minDurationMinutes = null;
        unwatchedOnly = false;
        partiallyWatchedOnly = false;
        titleQuery = "";
    }

    public synchronized boolean isActive() {
        return beforeTimestamp != null || maxDurationMinutes != null || minDurationMinutes != null
                || unwatchedOnly || partiallyWatchedOnly || !titleQuery.trim().isEmpty();
    }

    public Long getBeforeTimestamp() {
        return beforeTimestamp;
    }

    public void setBeforeTimestamp(Long beforeTimestamp) {
        this.beforeTimestamp = beforeTimestamp;
    }

    public Integer getMaxDurationMinutes() {
        return maxDurationMinutes;
    }

    public void setMaxDurationMinutes(Integer maxDurationMinutes) {
        this.maxDurationMinutes = maxDurationMinutes;
    }

    public Integer getMinDurationMinutes() {
        return minDurationMinutes;
    }

    public void setMinDurationMinutes(Integer minDurationMinutes) {
        this.minDurationMinutes = minDurationMinutes;
    }

    public boolean isUnwatchedOnly() {
        return unwatchedOnly;
    }

    public void setUnwatchedOnly(boolean unwatchedOnly) {
        this.unwatchedOnly = unwatchedOnly;
    }

    public boolean isPartiallyWatchedOnly() {
        return partiallyWatchedOnly;
    }

    public void setPartiallyWatchedOnly(boolean partiallyWatchedOnly) {
        this.partiallyWatchedOnly = partiallyWatchedOnly;
    }

    public String getTitleQuery() {
        return titleQuery;
    }

    public void setTitleQuery(String titleQuery) {
        this.titleQuery = titleQuery == null ? "" : titleQuery;
    }

    /**
     * Build the SQL conditions for the active filters so the database can apply them before
     * pagination.  Selection arguments are appended to {@code args} in the same order as the
     * placeholders in the returned clause.
     *
     * @param alias Table alias of the videos table in the query.
     */
    public synchronized String buildSqlWhere(String alias, List<String> args) {
        final List<String> conditions = new ArrayList<>();

        if (beforeTimestamp != null) {
            // A zero timestamp means the publish date is unknown, so it cannot be "before" a date.
            final String publishTime = alias + "." + SubscriptionsVideosTable.COL_PUBLISH_TIME.name();
            conditions.add(publishTime + " > 0 AND " + publishTime + " < ?");
            args.add(String.valueOf(beforeTimestamp));
        }
        if (maxDurationMinutes != null) {
            final String durationColumn = alias + "." + SubscriptionsVideosTable.COL_DURATION.name();
            conditions.add("(" + durationColumn + " > 0 AND " + durationColumn + " < ?)");
            args.add(String.valueOf(maxDurationMinutes * 60L));
        }
        if (minDurationMinutes != null) {
            conditions.add(alias + "." + SubscriptionsVideosTable.COL_DURATION.name() + " > ?");
            args.add(String.valueOf(minDurationMinutes * 60L));
        }

        final String query = titleQuery.trim().toLowerCase(Locale.getDefault());
        if (!query.isEmpty()) {
            conditions.add("LOWER(" + alias + "." + SubscriptionsVideosTable.COL_TITLE.name()
                    + ") LIKE ? ESCAPE '\\'");
            args.add("%" + escapeLike(query) + "%");
        }

        if (unwatchedOnly || partiallyWatchedOnly) {
            conditions.add(buildPlaybackSqlWhere(alias + "."
                            + SubscriptionsVideosTable.COL_YOUTUBE_VIDEO_ID_V2.name(),
                    unwatchedOnly, partiallyWatchedOnly));
        }

        return String.join(" AND ", conditions);
    }

    private static String escapeLike(String value) {
        return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private static String buildPlaybackSqlWhere(String videoIdColumn,
                                                boolean unwatched, boolean partiallyWatched) {
        final String historyTable = PlaybackStatusTable.TABLE_NAME;
        final String idColumn = PlaybackStatusTable.COL_YOUTUBE_VIDEO_ID;
        final String positionColumn = PlaybackStatusTable.COL_YOUTUBE_VIDEO_POSITION;
        final String watchedColumn = PlaybackStatusTable.COL_YOUTUBE_VIDEO_WATCHED;
        final String join = " FROM " + historyTable + " p WHERE p." + idColumn + " = "
                + videoIdColumn;

        if (unwatched && partiallyWatched) {
            // Not fully watched covers both unseen and partially watched videos.
            return "NOT EXISTS (SELECT 1" + join + " AND COALESCE(p." + watchedColumn + ",0) = 1)";
        }
        final String started = "(p." + positionColumn + " > 0 OR p." + watchedColumn + " = 1)";
        if (unwatched) {
            return "NOT EXISTS (SELECT 1" + join + " AND " + started + ")";
        }
        return "EXISTS (SELECT 1" + join + " AND " + started
                + " AND COALESCE(p." + watchedColumn + ",0) = 0)";
    }

}
