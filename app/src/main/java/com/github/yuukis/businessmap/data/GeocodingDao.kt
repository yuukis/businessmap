/*
 * GeocodingDao.kt
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

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface GeocodingDao {

    @Query("SELECT * FROM geocoding WHERE hash = :hash LIMIT 1")
    fun findByHash(hash: Int): GeocodingEntity?

    @Insert
    fun insert(entity: GeocodingEntity): Long

    @Query("UPDATE geocoding SET hash = hash WHERE hash = :hash")
    fun touchByHash(hash: Int): Int

    @Query("UPDATE geocoding SET latitude = :latitude, longitude = :longitude WHERE hash = :hash")
    fun updateLatLngByHash(hash: Int, latitude: Double, longitude: Double): Int
}
