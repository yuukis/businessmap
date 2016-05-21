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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.MapStatePreferences;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.view.OnInfoWindowElemTouchListener;
import com.github.yuukis.businessmap.widget.MapWrapperLayout;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.CancelableCallback;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ContactsMapFragment extends SupportMapFragment implements
		GoogleMap.OnInfoWindowClickListener {

	private GoogleMap mMap;
	private MapWrapperLayout mMapWrapperLayout;
	private View mInfoWindow;
	private Button mInfoButton;
	private OnInfoWindowElemTouchListener mInfoButtonListener;
	private SparseArray<Marker> mContactMarkerHashMap;
	private SparseArray<ContactsItem> mMarkerContactHashMap;
	private SparseArray<List<ContactsItem>> mLatlngContactsHashMap;
	private MapStatePreferences mPreferences;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContactMarkerHashMap = new SparseArray<Marker>();
		mMarkerContactHashMap = new SparseArray<ContactsItem>();
		mLatlngContactsHashMap = new SparseArray<List<ContactsItem>>();
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
		SparseArray<Marker> removeMarkerMap = cloneSparseArray(mContactMarkerHashMap);
		if (list == null) {
			return;
		}

		for (ContactsItem contacts : list) {
			if (removeMarkerMap.get(contacts.hashCode()) == null) {
				Marker marker = createMarker(contacts);
				if (marker != null) {
					LatLng position = marker.getPosition();
					List<ContactsItem> contactList = mLatlngContactsHashMap.get(position.hashCode());
					if (contactList == null) {
						contactList = new ArrayList<ContactsItem>();
					}
					if (!contactList.contains(contacts)) {
						contactList.add(contacts);
					}

					mContactMarkerHashMap.put(contacts.hashCode(), marker);
					mMarkerContactHashMap.put(marker.hashCode(), contacts);
					mLatlngContactsHashMap.put(position.hashCode(), contactList);
				}
			} else {
				removeMarkerMap.remove(contacts.hashCode());
			}
		}

		// 存在しない連絡先を地図から削除
		for (int i = 0; i < removeMarkerMap.size(); i++) {
			int key = removeMarkerMap.keyAt(i);
			Marker marker = removeMarkerMap.get(key);
			if (marker != null) {
				ContactsItem contacts = mMarkerContactHashMap.get(marker.hashCode());
				if (contacts != null) {
					mContactMarkerHashMap.remove(contacts.hashCode());
				}

				LatLng position = marker.getPosition();
				List<ContactsItem> contactList = mLatlngContactsHashMap.get(position.hashCode());
				if (contactList == null) {
					contactList = new ArrayList<ContactsItem>();
				}
				contactList.remove(contacts);
				mLatlngContactsHashMap.put(position.hashCode(), contactList);

				mMarkerContactHashMap.remove(marker.hashCode());
				marker.remove();
				marker = null;
			}
		}
		removeMarkerMap.clear();
	}

	public boolean showMarkerInfoWindow(ContactsItem contact, boolean animate) {
		if (mMap == null || contact == null) {
			return false;
		}
		final Marker marker = mContactMarkerHashMap.get(contact.hashCode());
		if (marker == null) {
			return false;
		}
		if (animate) {
			mMap.animateCamera(
				CameraUpdateFactory.newCameraPosition(
					new CameraPosition.Builder()
						.target(marker.getPosition())
						.zoom(15.5f)
						.build()
				),
				new CancelableCallback() {
					@Override
					public void onCancel() {
					}

					@Override
					public void onFinish() {
						marker.showInfoWindow();
					}
				}
			);
		} else {
			marker.showInfoWindow();
		}
		return true;
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		final ContactsItem contact = mMarkerContactHashMap.get(marker.hashCode());
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
				setUpMapInfoWindow();
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

	private SparseArray<Marker> cloneSparseArray(SparseArray<Marker> array) {
		SparseArray<Marker> clone = null;
		int sdk = Build.VERSION.SDK_INT;
		if (sdk < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			clone = new SparseArray<Marker>();
			for (int i = 0; i < array.size(); i++) {
				int key = array.keyAt(i);
				Marker marker = array.valueAt(i);
				clone.put(key, marker);
			}
		} else {
			clone = array.clone();
		}
		return clone;
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

	private void setUpMapInfoWindow() {
		mMapWrapperLayout = (MapWrapperLayout) getActivity()
				.findViewById(R.id.map_relative_layout);
		mMapWrapperLayout.init(getMap(), getPixelsFromDp(getActivity(), 39 + 20));

		mInfoWindow = getActivity().getLayoutInflater().inflate(
				R.layout.marker_info_contents, null);
		mInfoButton = (Button) mInfoWindow.findViewById(R.id.other_count);
		mInfoButtonListener = new OnInfoWindowElemTouchListener(
				mInfoButton,
				getResources().getDrawable(R.drawable.infowindow_button_normal),
				getResources().getDrawable(R.drawable.infowindow_button_pressed)) {
			@Override
			protected void onClickConfirmed(View v, Marker marker) {
				LatLng position = marker.getPosition();
				List<ContactsItem> contactsList = mLatlngContactsHashMap.get(position.hashCode());
				ContactsItemsDialogFragment.showDialog(getActivity(), contactsList);
			}
		};
		mInfoButton.setOnTouchListener(mInfoButtonListener);
	}

	private static int getPixelsFromDp(Context context, float dp) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

	private class MyInfoWindowAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			final ContactsItem contacts = mMarkerContactHashMap.get(marker.hashCode());
			final LatLng position = marker.getPosition();
			final List<ContactsItem> samePositionContacts = mLatlngContactsHashMap
					.get(position.hashCode());

			View view = mInfoWindow;
			TextView tvTitle = (TextView) view.findViewById(R.id.title);
			TextView tvCompanyName = (TextView) view.findViewById(R.id.company_name);
			TextView tvSnippet = (TextView) view.findViewById(R.id.snippet);
			TextView tvNote = (TextView) view.findViewById(R.id.note);
			Button btnOtherCount = mInfoButton;
			View separator = view.findViewById(R.id.separator);
			mInfoButtonListener.setMarker(marker);

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

				if (samePositionContacts != null && samePositionContacts.size() > 1) {
					String otherCount = getString(R.string.message_other_items);
					otherCount = String.format(Locale.getDefault(), otherCount,
							samePositionContacts.size() - 1);
					btnOtherCount.setText(otherCount);
					btnOtherCount.setVisibility(View.VISIBLE);
				} else {
					btnOtherCount.setVisibility(View.GONE);
				}
			}

			if (mMapWrapperLayout != null) {
				mMapWrapperLayout.setMarkerWithInfoWindow(marker, view);
			}

			return view;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}

	}

}
