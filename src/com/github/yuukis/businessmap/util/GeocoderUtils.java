package com.github.yuukis.businessmap.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.util.List;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
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

	public static Address getFromLocationLatLng(Context context, double lat, double lng)
			throws IOException, JSONException {
		Address addr = null;

		try {
			List<Address> list = new Geocoder(context, Locale.getDefault())
					.getFromLocation(lat, lng, 1);
			if (list.size() != 0) {
				addr = list.get(0);
			}
		} catch (IOException e) {
			//addr = getFromLocationNameToGoogleMaps(address);
		}
		return addr;
	}

	private static Double[] getFromLocationNameToGoogleMaps(String address)
			throws IOException, JSONException {
		String url = "http://maps.google.com/maps/api/geocode/json?address=%s&ka&sensor=false";
		address = URLEncoder.encode(address, "UTF-8");
		url = String.format(Locale.getDefault(), url, address);
		HttpGet httpGet = new HttpGet(url);
		HttpClient client = new DefaultHttpClient();
		HttpResponse response;
		StringBuilder stringBuilder = new StringBuilder();

		try {
			response = client.execute(httpGet);
			HttpEntity entity = response.getEntity();
			InputStream stream = entity.getContent();
			int b;
			while ((b = stream.read()) != -1) {
				stringBuilder.append((char) b);
			}
		} catch (ClientProtocolException e) {
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
