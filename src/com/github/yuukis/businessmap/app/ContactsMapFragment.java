/*
 * ContactsMapFragment.java
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
package com.github.yuukis.businessmap.app;

import java.util.List;

import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.MapStatePreferences;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ContactsMapFragment extends MapFragment implements
		GoogleMap.OnInfoWindowClickListener {

	private final static float[] MARKER_HUE_LIST = new float[]{
		0.0f,
		30.0f,
		60.0f,
		90.0f,
		180.0f,
		210.0f,
		270.0f,
		300.0f,
	};

	private SparseArray<Marker> mMarkerHashMap;
	private SparseArray<ContactsItem> mContactHashMap;
	private MapStatePreferences mPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMarkerHashMap = new SparseArray<Marker>();
		mContactHashMap = new SparseArray<ContactsItem>();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		mPreferences = new MapStatePreferences(getActivity());
		CameraPosition position = mPreferences.getCameraPosition();

		GoogleMap map = getMap();
		map.setInfoWindowAdapter(new MyInfoWindowAdapter());
		map.setOnInfoWindowClickListener(this);
		map.setIndoorEnabled(true);
		map.setMyLocationEnabled(true);
		map.moveCamera(CameraUpdateFactory.newCameraPosition(position));
	}

	@Override
	public void onDestroyView() {
		CameraPosition position = getMap().getCameraPosition();
		mPreferences.setCameraPosition(position);

		super.onDestroyView();
	}

	public void notifyDataSetChanged() {
		getMap().clear();
		mMarkerHashMap.clear();
		mContactHashMap.clear();
		List<ContactsItem> list = getContactsList();
		if (list == null) {
			return;
		}
		for (ContactsItem contact : list) {
			if (contact.getLat() == null || contact.getLng() == null) {
				continue;
			}
			String name = contact.getName();
			if (name == null) {
				name = getString(R.string.message_no_data);
			}
			String address = contact.getAddress();
			if (address == null) {
				address = getString(R.string.message_no_data);
			}
			LatLng latLng = new LatLng(contact.getLat(), contact.getLng());
			int hueIndex = (int) (Math.random() * MARKER_HUE_LIST.length);
			float hue = MARKER_HUE_LIST[hueIndex % MARKER_HUE_LIST.length];
			Marker marker = getMap().addMarker(new MarkerOptions()
					.position(latLng)
					.title(name)
					.snippet(address)
					.icon(BitmapDescriptorFactory.defaultMarker(hue)));
			mMarkerHashMap.put(contact.hashCode(), marker);
			mContactHashMap.put(marker.hashCode(), contact);
		}
	}

	public boolean showMarkerInfoWindow(ContactsItem contact) {
		final Marker marker = mMarkerHashMap.get(contact.hashCode());
		if (marker == null) {
			return false;
		}
		getMap().animateCamera(
				CameraUpdateFactory.newCameraPosition(
				new CameraPosition.Builder()
						.target(marker.getPosition())
						.zoom(15.5f)
						.build()),
				new CancelableCallback() {
					@Override
					public void onCancel() {
					}

					@Override
					public void onFinish() {
						marker.showInfoWindow();
					}
				});
		return true;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		final ContactsItem contact = mContactHashMap.get(marker.hashCode());
		if (contact == null) {
			return;
		}
		ContactsActionFragment.showDialog(getActivity(), contact);
	}

	private List<ContactsItem> getContactsList() {
		MainActivity activity = (MainActivity) getActivity();
		return activity.getCurrentContactsList();
	}

	private class MyInfoWindowAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			ContactsItem contacts = mContactHashMap.get(marker.hashCode());
			View view = getActivity().getLayoutInflater().inflate(
					R.layout.marker_info_contents, null);
			TextView tvTitle = (TextView) view.findViewById(R.id.title);
			TextView tvSnippet = (TextView) view.findViewById(R.id.snippet);
			TextView tvNote = (TextView) view.findViewById(R.id.note);
			View separator = view.findViewById(R.id.separator);

			if (contacts != null) {
				String title = marker.getTitle();
				tvTitle.setText(title);

				String snippet = marker.getSnippet();
				snippet = snippet.replaceAll("[ 　]", "\n");
				tvSnippet.setText(snippet);

				String note = contacts.getNote();
				if (TextUtils.isEmpty(note)) {
					separator.setVisibility(View.GONE);
					tvNote.setVisibility(View.GONE);
				} else {
					tvNote.setText(note);
					separator.setVisibility(View.VISIBLE);
					tvNote.setVisibility(View.VISIBLE);
				}
			}
			return view;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}

	}

}
