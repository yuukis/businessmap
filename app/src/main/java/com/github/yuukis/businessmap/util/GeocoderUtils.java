package com.github.yuukis.businessmap.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;

public class GeocoderUtils {

	public static Double[] getFromLocationName(Context context, String address)
			throws IOException, JSONException {
		Double[] latlng = null;
		double lat = Double.NaN;
		double lng = Double.NaN;

		try {
			List<Address> list = new Geocoder(context, Locale.getDefault())
					.getFromLocationName(address, 1);
			if (list.size() != 0) {
				Address addr = list.get(0);
				lat = addr.getLatitude();
				lng = addr.getLongitude();
			}
			latlng = new Double[] { lat, lng };
		} catch (IOException e) {
			latlng = getFromLocationNameToGoogleMaps(address);
		}
		return latlng;
	}

	private static Double[] getFromLocationNameToGoogleMaps(String address)
			throws IOException, JSONException {
		String urlFormat = "https://maps.google.com/maps/api/geocode/json?address=%s&ka&sensor=false";
		address = URLEncoder.encode(address, "UTF-8");
		String url = String.format(Locale.getDefault(), urlFormat, address);
		StringBuilder stringBuilder = new StringBuilder();

		HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
		try {
			InputStream stream = connection.getInputStream();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} finally {
			connection.disconnect();
		}

		JSONObject jsonObject = new JSONObject(stringBuilder.toString());
		double lat = Double.NaN;
		double lng = Double.NaN;
		try {
			lat = ((JSONArray) jsonObject.get("results"))
					.getJSONObject(0)
					.getJSONObject("geometry")
					.getJSONObject("location")
					.getDouble("lat");
			lng = ((JSONArray) jsonObject.get("results"))
					.getJSONObject(0)
					.getJSONObject("geometry")
					.getJSONObject("location")
					.getDouble("lng");
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Double[] latlng = new Double[] { lat, lng };
		return latlng;
	}

}
