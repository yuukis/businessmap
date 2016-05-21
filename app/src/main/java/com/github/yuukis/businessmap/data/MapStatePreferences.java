/*
 * MapStatePreferences.java
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

import android.content.Context;
import android.content.SharedPreferences;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapStatePreferences {

	private static final String PREF_NAME = "map";
	private static final String KEY_LAT = "latitude";
	private static final String KEY_LNG = "longitude";
	private static final String KEY_ZOOM = "zoom";
	private static final String KEY_TILT = "tilt";
	private static final String KEY_BEARING = "bearing";
	private static final float DEFAULT_LAT = Float.NaN;
	private static final float DEFAULT_LNG = Float.NaN;
	private static final float DEFAULT_ZOOM = 12;

	private Context mContext;

	public MapStatePreferences(Context context) {
		mContext = context;
	}

	public CameraPosition getCameraPosition() {
		SharedPreferences preferences = getSharedPreferences();
		float lat = preferences.getFloat(KEY_LAT, DEFAULT_LAT);
		float lng = preferences.getFloat(KEY_LNG, DEFAULT_LNG);
		float zoom = preferences.getFloat(KEY_ZOOM, DEFAULT_ZOOM);
		float tilt = preferences.getFloat(KEY_TILT, 0);
		float bearing = preferences.getFloat(KEY_BEARING, 0);

		LatLng target;
		if (Float.isNaN(lat) || Float.isNaN(lng)) {
			target = getLastKnownLocation();
		} else {
			target = new LatLng(lat, lng);
		}
		CameraPosition position = new CameraPosition(target, zoom, tilt, bearing);

		return position;
	}

	public MapStatePreferences setCameraPosition(CameraPosition position) {
		float lat = (float) position.target.latitude;
		float lng = (float) position.target.longitude;
		float zoom = position.zoom;
		float tilt = position.tilt;
		float bearing = position.bearing;

		getSharedPreferences().edit()
				.putFloat(KEY_LAT, lat)
				.putFloat(KEY_LNG, lng)
				.putFloat(KEY_ZOOM, zoom)
				.putFloat(KEY_TILT, tilt)
				.putFloat(KEY_BEARING, bearing)
				.commit();

		return this;
	}

	private SharedPreferences getSharedPreferences() {
		return mContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
	}

	private LatLng getLastKnownLocation() {
		LocationManager manager = (LocationManager) mContext
				.getSystemService(Context.LOCATION_SERVICE);
		Criteria criteria = new Criteria();
		criteria.setAccuracy(Criteria.ACCURACY_FINE);
		String provider = manager.getBestProvider(criteria, true);
		provider = LocationManager.NETWORK_PROVIDER;
		Location location = manager.getLastKnownLocation(provider);
		// 位置情報取得に失敗した場合の既定値
		double lat = 139.766084, lng = 35.681382;	// 東京駅
		if (location != null) {
			lat = location.getLatitude();
			lng = location.getLongitude();
		}
		return new LatLng(lat, lng);
	}
}
