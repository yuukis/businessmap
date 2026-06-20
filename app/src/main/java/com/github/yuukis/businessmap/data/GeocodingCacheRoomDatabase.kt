/*
 * GeocodingCacheRoomDatabase.kt
 *
 * Copyright 2026 Yuuki Shimizu.
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

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [GeocodingEntity::class], version = GeocodingCacheRoomDatabase.VERSION, exportSchema = false)
abstract class GeocodingCacheRoomDatabase : RoomDatabase() {

    abstract fun geocodingDao(): GeocodingDao

    companion object {
        private const val DATABASE_NAME = "business_map"
        const val VERSION = 2

        // [v1 -> v2] lat:0, lng:0 で登録されているレコードを一旦削除する
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("DELETE FROM geocoding WHERE latitude = 0 AND longitude = 0")
            }
        }

        fun create(context: Context): GeocodingCacheRoomDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                GeocodingCacheRoomDatabase::class.java,
                DATABASE_NAME
            )
                .addMigrations(MIGRATION_1_2)
                .build()
        }
    }
}
