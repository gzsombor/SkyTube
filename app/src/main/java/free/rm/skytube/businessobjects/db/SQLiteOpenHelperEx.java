/*
 * SkyTube
 * Copyright (C) 2017  Ramon Mifsud
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

import java.io.File;

import free.rm.skytube.app.SkyTubeApp;

/**
 * Base class for all the database facades.
 *
 * <p>Every table of the application is stored in the one, shared {@code subs.db} database. The
 * schema and the migrations are owned by {@link SubscriptionsDb}, so the other helpers only need
 * to delegate their database access to it. This keeps the existing, table-oriented APIs intact
 * while ensuring that only one {@link android.database.sqlite.SQLiteOpenHelper} manages the file
 * (and therefore its version).</p>
 */
public abstract class SQLiteOpenHelperEx {

	/**
	 * @return The database (full) path of the shared database.
	 */
	public String getDatabasePath() {
		return SkyTubeApp.getContext().getDatabasePath(SubscriptionsDb.DATABASE_NAME).getPath();
	}


	/**
	 * @return The directory which contains the shared database (as a {@link File}).
	 */
	public File getDatabaseDirectory() {
		return SkyTubeApp.getContext().getDatabasePath(SubscriptionsDb.DATABASE_NAME).getParentFile();
	}


	public SQLiteDatabase getReadableDatabase() {
		return SubscriptionsDb.getSubscriptionsDb().getReadableDatabase();
	}


	public SQLiteDatabase getWritableDatabase() {
		return SubscriptionsDb.getSubscriptionsDb().getWritableDatabase();
	}


	/**
	 * Close the shared database. All the facades refer to the same file, so closing any of them
	 * closes the connection used by all of them.
	 */
	public void close() {
		SubscriptionsDb.getSubscriptionsDb().close();
	}
}
