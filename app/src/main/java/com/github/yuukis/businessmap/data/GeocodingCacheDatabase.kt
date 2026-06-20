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

import android.content.Context
import android.database.SQLException

class GeocodingCacheDatabase(context: Context) {

    private var database: GeocodingCacheRoomDatabase? = GeocodingCacheRoomDatabase.create(context)

    fun get(address: String): DoubleArray? {
        val dao = database?.geocodingDao() ?: return null
        val entity = dao.findByHash(address.hashCode()) ?: return null
        val lat = entity.latitude ?: Double.NaN
        val lng = entity.longitude ?: Double.NaN
        return doubleArrayOf(lat, lng)
    }

    fun put(address: String, latlng: Array<Double?>?): Boolean {
        if (latlng == null || latlng.size != 2) {
            return false
        }
        val dao = database?.geocodingDao() ?: return false
        val hash = address.hashCode()
        val lat = latlng[0] ?: Double.NaN
        val lng = latlng[1] ?: Double.NaN
        val hasValidLatLng = !lat.isNaN() && !lng.isNaN()

        try {
            val updated = if (hasValidLatLng) {
                dao.updateLatLngByHash(hash, lat, lng) > 0
            } else {
                dao.touchByHash(hash) > 0
            }
            if (!updated) {
                dao.insert(
                    GeocodingEntity(
                        hash = hash,
                        latitude = if (hasValidLatLng) lat else null,
                        longitude = if (hasValidLatLng) lng else null
                    )
                )
            }
        } catch (e: SQLException) {
            return false
        }

        return true
    }

    fun close() {
        val db = database
        if (db != null && db.isOpen) {
            db.close()
            database = null
        }
    }
}
