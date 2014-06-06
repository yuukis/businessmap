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
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ContactsMapFragment extends MapFragment implements
		GoogleMap.OnInfoWindowClickListener {

	private GoogleMap mMap;
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
		setUpMapIfNeeded();
	}

	@Override
	public void onResume() {
		super.onResume();
		setUpMapIfNeeded();
	}

	@Override
	public void onDestroyView() {
		if (mMap != null) {
			CameraPosition position = mMap.getCameraPosition();
			mPreferences.setCameraPosition(position);
		}

		super.onDestroyView();
	}

	public void notifyDataSetChanged() {
		if (mMap == null) {
			return;
		}
		List<ContactsItem> list = getContactsList();
		SparseArray<Marker> removeMarkerHashMap = mMarkerHashMap.clone();
		if (list == null) {
			return;
		}

		for (ContactsItem contacts : list) {
			if (removeMarkerHashMap.get(contacts.hashCode()) == null) {
				Marker marker = createMarker(contacts);
				if (marker != null) {
					mMarkerHashMap.put(contacts.hashCode(), marker);
					mContactHashMap.put(marker.hashCode(), contacts);
				}
			} else {
				removeMarkerHashMap.remove(contacts.hashCode());
			}
		}

		// 存在しない連絡先を地図から削除
		for (int i = 0; i < removeMarkerHashMap.size(); i++) {
			int key = removeMarkerHashMap.keyAt(i);
			Marker marker = removeMarkerHashMap.get(key);
			if (marker != null) {
				ContactsItem contacts = mContactHashMap.get(marker.hashCode());
				if (contacts != null) {
					mMarkerHashMap.remove(contacts.hashCode());
				}
				mContactHashMap.remove(marker.hashCode());
				marker.remove();
				marker = null;
			}
		}
		removeMarkerHashMap.clear();
	}

	private Marker createMarker(ContactsItem contacts) {
		if (contacts.getLat() == null || contacts.getLng() == null) {
			return null;
		}
		String name = contacts.getName();
		if (name == null) {
			name = getString(R.string.message_no_data);
		}
		String address = contacts.getAddress();
		if (address == null) {
			address = getString(R.string.message_no_data);
		}
		LatLng latLng = new LatLng(contacts.getLat(), contacts.getLng());
		Marker marker = mMap.addMarker(new MarkerOptions()
				.position(latLng)
				.title(name)
				.snippet(address));
		return marker;
	}

	public boolean showMarkerInfoWindow(ContactsItem contact) {
		if (mMap == null) {
			return false;
		}
		final Marker marker = mMarkerHashMap.get(contact.hashCode());
		if (marker == null) {
			return false;
		}
		mMap.animateCamera(
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

	private void setUpMapIfNeeded() {
		if (mMap == null) {
			mMap = getMap();
			if (mMap != null) {
				setUpMap();
			}
		}
	}

	private void setUpMap() {
		CameraPosition position = mPreferences.getCameraPosition();
		mMap.setInfoWindowAdapter(new MyInfoWindowAdapter());
		mMap.setOnInfoWindowClickListener(this);
		mMap.setIndoorEnabled(false);
		mMap.setMyLocationEnabled(true);
		mMap.moveCamera(CameraUpdateFactory.newCameraPosition(position));
	}

	private class MyInfoWindowAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			ContactsItem contacts = mContactHashMap.get(marker.hashCode());
			View view = getActivity().getLayoutInflater().inflate(
					R.layout.marker_info_contents, null);
			TextView tvTitle = (TextView) view.findViewById(R.id.title);
			TextView tvCompanyName = (TextView) view.findViewById(R.id.company_name);
			TextView tvSnippet = (TextView) view.findViewById(R.id.snippet);
			TextView tvNote = (TextView) view.findViewById(R.id.note);
			View separator = view.findViewById(R.id.separator);

			if (contacts != null) {
				String title = marker.getTitle();
				tvTitle.setText(title);

				String companyName = contacts.getCompanyName();
				if (TextUtils.isEmpty(companyName)) {
					tvCompanyName.setVisibility(View.GONE);
				} else {
					tvCompanyName.setText(companyName);
					tvCompanyName.setVisibility(View.VISIBLE);
				}

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
