package free.rm.skytube.businessobjects.db;

import com.github.skytube.components.utils.Column;

/**
 * Downloaded Videos Table
 */
public class DownloadedVideosTable {
	public static final String TABLE_NAME = "DownloadedVideos";
	public static final String COL_YOUTUBE_VIDEO_ID = "YouTube_Video_Id";
	public static final String COL_YOUTUBE_VIDEO = "YouTube_Video";
	public static final String COL_FILE_URI = "File_URI";
	public static final String COL_AUDIO_FILE_URI = "Audio_URI";
	public static final String COL_SB = "SponsorBlock";
	public static final String COL_ORDER = "Order_Index";
	/** Flat columns used by the feed filters, so filtering can happen in SQL before pagination. */
	public static final Column COL_TITLE = new Column("title", "text");
	public static final Column COL_DURATION = new Column("duration", "integer", "not null default 0");
	public static final Column COL_PUBLISH_TIME = new Column("publish_time", "integer", "not null default 0");

	static final String MAXIMUM_ORDER_QUERY = String.format("SELECT MAX(%s) FROM %s", COL_ORDER, TABLE_NAME);
	static final String COUNT_ALL = String.format("SELECT COUNT(*) FROM %s", TABLE_NAME);

	private static final String ADD_COLUMN = "ALTER TABLE " + TABLE_NAME + " ADD COLUMN ";

	public static String getCreateStatement() {
		return "CREATE TABLE " + TABLE_NAME + " (" +
						COL_YOUTUBE_VIDEO_ID + " TEXT PRIMARY KEY NOT NULL, " +
						COL_YOUTUBE_VIDEO + " BLOB, " +
						COL_FILE_URI + " TEXT, " +
						COL_AUDIO_FILE_URI + " TEXT, " +
						COL_SB + " BLOB, " +
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
