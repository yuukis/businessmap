/*
 * GeocodingCacheDatabase.java
 *
 * Copyright 2013 Yuuki Shimizu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yuukis.businessmap.data;

import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.provider.BaseColumns;

public class GeocodingCacheDatabase {

	private static final String TABLE_NAME = "geocoding";

	private SQLiteDatabase db;

	public interface DataColumns extends BaseColumns {
		public static final String ID = "_id";
		public static final String HASH = "hash";
		public static final String LAT = "latitude";
		public static final String LNG = "longitude";
	}

	public GeocodingCacheDatabase(Context context) {
		DatabaseHelper dbHelper = new DatabaseHelper(context);
		db = dbHelper.getWritableDatabase();
	}

	public double[] get(String address) {
		final int hash = address.hashCode();
		final String[] columns = {
				DataColumns.LAT,
				DataColumns.LNG };
		final String selection = String.format(Locale.getDefault(),
				"%s=%d", DataColumns.HASH, hash);
		Cursor cursor = query(columns, selection, null, null);

		double[] latlng = null;
		if (cursor.moveToNext()) {
			double lat = cursor.getDouble(0);
			double lng = cursor.getDouble(1);
			latlng = new double[] {lat, lng};
		}
		return latlng;
	}

	public boolean put(String address, Double[] latlng) {
		if (latlng == null || latlng.length != 2) {
			return false;
		}
		int hash = address.hashCode();
		double lat = latlng[0];
		double lng = latlng[1];

		ContentValues values = new ContentValues();
		values.put(DataColumns.HASH, hash);
		values.put(DataColumns.LAT, lat);
		values.put(DataColumns.LNG, lng);
		try {
			if (!update(values, hash)) {
				insert(values);
			}
		} catch (SQLException e) {
			return false;
		}

		return true;
	}

	public void close() {
		if (db != null && db.isOpen()) {
			db.close();
		}
	}

	private Cursor query(String[] columns, String selection,
			String[] selectionArgs, String sortOrder) {
		return db.query(TABLE_NAME, columns, selection, selectionArgs, null,
				null, sortOrder);
	}

	private boolean update(ContentValues values, int hash) {
		String whereClause = String.format(Locale.getDefault(), "%s=%d",
				DataColumns.HASH, hash);
		int affected = db.update(TABLE_NAME, values, whereClause, null);
		return affected > 0;
	}

	private long insert(ContentValues values) throws SQLException {
		long rowId = db.insert(TABLE_NAME, null, values);

		if (rowId > 0) {
			return rowId;
		}

		throw new SQLException();
	}

	private class DatabaseHelper extends SQLiteOpenHelper {

		private static final String DATABASE_NAME = "business_map";
		private final static int VERSION = 1;

		public DatabaseHelper(Context context) {
			super(context, DATABASE_NAME, null, VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			StringBuffer sb = new StringBuffer();
			sb.append("CREATE TABLE " + TABLE_NAME + "(");
			sb.append(DataColumns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, ");
			sb.append(DataColumns.HASH + " INTEGER, ");
			sb.append(DataColumns.LAT + " REAL, ");
			sb.append(DataColumns.LNG + " REAL ");
			sb.append(")");
			String sql = sb.toString();
			db.execSQL(sql);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

}
