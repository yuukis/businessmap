package com.github.yuukis.businessmap.data;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;

public class MapStatePreferences {

	private static final String PREF_NAME = "map";
	private static final String KEY_LAT = "latitude";
	private static final String KEY_LNG = "longitude";
	private static final String KEY_ZOOM = "zoom";
	private static final String KEY_TILT = "tilt";
	private static final String KEY_BEARING = "bearing";

	private Context mContext;

	public MapStatePreferences(Context context) {
		mContext = context;
	}

	public CameraPosition getCameraPosition() {
		SharedPreferences preferences = getSharedPreferences();
		float lat = preferences.getFloat(KEY_LAT, 0);
		float lng = preferences.getFloat(KEY_LNG, 0);
		float zoom = preferences.getFloat(KEY_ZOOM, 0);
		float tilt = preferences.getFloat(KEY_TILT, 0);
		float bearing = preferences.getFloat(KEY_BEARING, 0);

		LatLng target = new LatLng(lat, lng);
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
}
