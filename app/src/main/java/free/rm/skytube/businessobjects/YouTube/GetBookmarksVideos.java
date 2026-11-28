/*
 * SkyTube
 * Copyright (C) 2018  Ramon Mifsud
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

package free.rm.skytube.businessobjects.YouTube;

import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

import free.rm.skytube.app.Utils;
import free.rm.skytube.businessobjects.FeedFilter;
import free.rm.skytube.businessobjects.YouTube.POJOs.CardData;
import free.rm.skytube.businessobjects.YouTube.POJOs.YouTubeVideo;
import free.rm.skytube.businessobjects.db.BookmarksDb;


/**
 * Get user's bookmarked videos.
 */
public class GetBookmarksVideos extends GetYouTubeVideos {
    private static final int PAGE_SIZE = 20;

    private Integer minOrder;

    @Override
    public void init() {
        reset();
    }

    @Override
    public void reset() {
        minOrder = null;
        noMoreVideoPages = false;
    }

    @Override
    public List<CardData> getNextVideos() {
        if (noMoreVideoPages()) {
            return null;
        }

        // The filters are applied by BookmarksDb in SQL, before the page limit.
        final BookmarksDb db = BookmarksDb.getBookmarksDb();
        final Pair<List<YouTubeVideo>, Integer> bookmarkedVideos =
                db.getBookmarkedVideos(PAGE_SIZE, minOrder, FeedFilter.getInstance());
        if (bookmarkedVideos.first.isEmpty()) {
            noMoreVideoPages = true;
        } else {
            minOrder = Utils.min(bookmarkedVideos.second, minOrder);
        }
        return new ArrayList<>(bookmarkedVideos.first);
    }

}
