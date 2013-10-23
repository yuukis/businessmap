/*
 * GeocoderUtils.java
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
package com.github.yuukis.businessmap.utils;

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
			throws IOException {
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
			throws IOException {
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

		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject(stringBuilder.toString());
		} catch (JSONException e) {
			e.printStackTrace();
		}
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
