/*
 * MapStatePreferences.kt
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

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng

class MapStatePreferences(private val context: Context) {

    fun getCameraPosition(): CameraPosition {
        val preferences = getSharedPreferences()
        val lat = preferences.getFloat(KEY_LAT, DEFAULT_LAT)
        val lng = preferences.getFloat(KEY_LNG, DEFAULT_LNG)
        val zoom = preferences.getFloat(KEY_ZOOM, DEFAULT_ZOOM)
        val tilt = preferences.getFloat(KEY_TILT, 0f)
        val bearing = preferences.getFloat(KEY_BEARING, 0f)

        val target = if (lat.isNaN() || lng.isNaN()) {
            getLastKnownLocation()
        } else {
            LatLng(lat.toDouble(), lng.toDouble())
        }

        return CameraPosition(target, zoom, tilt, bearing)
    }

    fun setCameraPosition(position: CameraPosition): MapStatePreferences {
        val lat = position.target.latitude.toFloat()
        val lng = position.target.longitude.toFloat()
        val zoom = position.zoom
        val tilt = position.tilt
        val bearing = position.bearing

        getSharedPreferences().edit()
            .putFloat(KEY_LAT, lat)
            .putFloat(KEY_LNG, lng)
            .putFloat(KEY_ZOOM, zoom)
            .putFloat(KEY_TILT, tilt)
            .putFloat(KEY_BEARING, bearing)
            .commit()

        return this
    }

    private fun getSharedPreferences(): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    @SuppressLint("MissingPermission")
    private fun getLastKnownLocation(): LatLng {
        // 位置情報取得に失敗した場合の既定値
        var lat = 35.681382
        var lng = 139.766084 // 東京駅

        if (hasLocationPermission()) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val location = manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                lat = location.latitude
                lng = location.longitude
            }
        }
        return LatLng(lat, lng)
    }

    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val PREF_NAME = "map"
        private const val KEY_LAT = "latitude"
        private const val KEY_LNG = "longitude"
        private const val KEY_ZOOM = "zoom"
        private const val KEY_TILT = "tilt"
        private const val KEY_BEARING = "bearing"
        private const val DEFAULT_LAT = Float.NaN
        private const val DEFAULT_LNG = Float.NaN
        private const val DEFAULT_ZOOM = 12f
    }
}
