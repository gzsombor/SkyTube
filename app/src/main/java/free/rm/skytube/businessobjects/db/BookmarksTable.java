/*
 * SkyTube
 * Copyright (C) 2016  Ramon Mifsud
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

import com.github.skytube.components.utils.Column;

/**
 * Bookmarked Videos Table
 */
public class BookmarksTable {
	public static final String TABLE_NAME = "Bookmarks";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_ORDER = "Order_Index";
	/** Flat columns used by the feed filters, so filtering can happen in SQL before pagination. */
	public static final Column COL_TITLE = new Column("title", "text");
	public static final Column COL_DURATION = new Column("duration", "integer", "not null default 0");
	public static final Column COL_PUBLISH_TIME = new Column("publish_time", "integer", "not null default 0");

	private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";


	static final String COUNT_ALL_BOOKMARKS = String.format("SELECT COUNT(*) FROM %s", BookmarksTable.TABLE_NAME);
	static final String MAXIMUM_ORDER_QUERY = String.format("SELECT MAX(%s) FROM %s", COL_ORDER, TABLE_NAME);
	static final String QUERY_ALL_IDS = String.format("SELECT %1$s FROM %2$s", COL_YOUTUBE_VIDEO_ID, TABLE_NAME);
	static final String IS_BOOKMARKED_QUERY = String.format("SELECT 1 FROM %s WHERE %s =?", TABLE_NAME, COL_YOUTUBE_VIDEO_ID);


	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_ORDER + " INTEGER " +
						" )";
	}

	public static String[] getAddFilterColumns() {
		return new String[]{
				ADD_COLUMN + COL_TITLE.format(),
				ADD_COLUMN + COL_DURATION.format(),
				ADD_COLUMN + COL_PUBLISH_TIME.format(),
		};
	}

}
