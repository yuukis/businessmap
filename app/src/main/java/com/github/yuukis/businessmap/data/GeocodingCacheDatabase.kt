/*
 * GeocodingCacheDatabase.kt
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
package com.github.yuukis.businessmap.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.SQLException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import android.util.Log
import java.util.Locale

class GeocodingCacheDatabase(context: Context) {

    private var db: SQLiteDatabase? = DatabaseHelper(context).writableDatabase

    interface DataColumns : BaseColumns {
        companion object {
            const val ID = "_id"
            const val HASH = "hash"
            const val LAT = "latitude"
            const val LNG = "longitude"
        }
    }

    fun get(address: String): DoubleArray? {
        val hash = address.hashCode()
        val columns = arrayOf(DataColumns.LAT, DataColumns.LNG)
        val selection = String.format(Locale.getDefault(), "%s=%d", DataColumns.HASH, hash)
        var latlng: DoubleArray? = null
        val cursor = query(columns, selection, null, null)
        try {
            if (cursor.moveToNext()) {
                val lat: Double
                val lng: Double
                if (cursor.isNull(0) || cursor.isNull(1)) {
                    lat = Double.NaN
                    lng = Double.NaN
                } else {
                    lat = cursor.getDouble(0)
                    lng = cursor.getDouble(1)
                }
                latlng = doubleArrayOf(lat, lng)
            }
        } finally {
            cursor.close()
        }
        return latlng
    }

    fun put(address: String, latlng: Array<Double?>?): Boolean {
        if (latlng == null || latlng.size != 2) {
            return false
        }
        val hash = address.hashCode()
        val lat = latlng[0] ?: Double.NaN
        val lng = latlng[1] ?: Double.NaN

        val values = ContentValues()
        values.put(DataColumns.HASH, hash)
        if (!lat.isNaN() && !lng.isNaN()) {
            values.put(DataColumns.LAT, lat)
            values.put(DataColumns.LNG, lng)
        }
        try {
            if (!update(values, hash)) {
                insert(values)
            }
        } catch (e: SQLException) {
            return false
        }

        return true
    }

    fun close() {
        val database = db
        if (database != null && database.isOpen) {
            // FIXME: SQLiteDatabase#closeDatabase で発生する場合がある NPE の回避
            try {
                database.close()
            } catch (e: NullPointerException) {
            }
            db = null
        }
    }

    private fun query(
        columns: Array<String>,
        selection: String,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor {
        return db!!.query(TABLE_NAME, columns, selection, selectionArgs, null, null, sortOrder)
    }

    private fun update(values: ContentValues, hash: Int): Boolean {
        val whereClause = String.format(Locale.getDefault(), "%s=%d", DataColumns.HASH, hash)
        val affected = db!!.update(TABLE_NAME, values, whereClause, null)
        return affected > 0
    }

    @Throws(SQLException::class)
    private fun insert(values: ContentValues): Long {
        val rowId = db!!.insert(TABLE_NAME, null, values)

        if (rowId > 0) {
            return rowId
        }

        throw SQLException()
    }

    private class DatabaseHelper(context: Context) :
        SQLiteOpenHelper(context, DATABASE_NAME, null, VERSION) {

        override fun onCreate(db: SQLiteDatabase) {
            val sb = StringBuilder()
            sb.append("CREATE TABLE $TABLE_NAME(")
            sb.append("${BaseColumns._ID} INTEGER PRIMARY KEY AUTOINCREMENT, ")
            sb.append("${DataColumns.HASH} INTEGER, ")
            sb.append("${DataColumns.LAT} REAL, ")
            sb.append("${DataColumns.LNG} REAL ")
            sb.append(")")
            db.execSQL(sb.toString())
        }

        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            Log.i(GeocodingCacheDatabase::class.java.simpleName, "Upgrade database.")

            // [v1 -> v2] lat:0, lng:0 で登録されているレコードを一旦削除する
            if (oldVersion < 2 && newVersion >= 2) {
                val sql = "DELETE FROM %s WHERE %s = 0 AND %s = 0"
                db.execSQL(String.format(sql, TABLE_NAME, DataColumns.LAT, DataColumns.LNG))
            }
        }

        companion object {
            private const val DATABASE_NAME = "business_map"
            private const val VERSION = 2
        }
    }

    companion object {
        private const val TABLE_NAME = "geocoding"
    }
}
