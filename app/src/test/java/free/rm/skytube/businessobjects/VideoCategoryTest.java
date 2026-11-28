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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Only database-backed feeds expose the feed filter button.
 */
public class VideoCategoryTest {

    @Test
    void feedFilterIsEnabledOnlyForDatabaseFeeds() {
        assertTrue(VideoCategory.SUBSCRIPTIONS_FEED_VIDEOS.isFeedFilteringEnabled());
        assertTrue(VideoCategory.BOOKMARKS_VIDEOS.isFeedFilteringEnabled());
        assertTrue(VideoCategory.DOWNLOADED_VIDEOS.isFeedFilteringEnabled());

        assertFalse(VideoCategory.FEATURED.isFeedFilteringEnabled());
        assertFalse(VideoCategory.MOST_POPULAR.isFeedFilteringEnabled());
        assertFalse(VideoCategory.SEARCH_QUERY.isFeedFilteringEnabled());
        assertFalse(VideoCategory.CHANNEL_VIDEOS.isFeedFilteringEnabled());
        assertFalse(VideoCategory.PLAYLIST_VIDEOS.isFeedFilteringEnabled());
        assertFalse(VideoCategory.MIXED_PLAYLIST_VIDEOS.isFeedFilteringEnabled());
    }
}
