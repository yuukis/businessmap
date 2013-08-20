package com.github.yuukis.businessmap.app;

import java.util.List;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.MapStatePreferences;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.utils.ActionUtils;
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
		for (ContactsItem contact : getContactsList()) {
			if (contact.getLat() == null || contact.getLng() == null) {
				continue;
			}
			String name = contact.getName();
			String address = contact.getAddress();
			if (address == null) {
				address = getString(R.string.message_no_address);
			}
			LatLng latLng = new LatLng(contact.getLat(), contact.getLng());
			Marker marker = getMap().addMarker(new MarkerOptions()
					.position(latLng)
					.title(name)
					.snippet(address));
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

		final Context context = getActivity();
		String title = contact.getName();
		final String[] items = new String[] {
				getString(R.string.action_contacts_detail),
				getString(R.string.action_directions),
				getString(R.string.action_drive_navigation)
		};
		new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							ActionUtils.doShowContact(context, contact);
							break;
						case 1:
							ActionUtils.doShowDirections(context, contact);
							break;
						case 2:
							ActionUtils
									.doStartDriveNavigation(context, contact);
						}
					}
				})
				.show();
	}

	private List<ContactsItem> getContactsList() {
		MainActivity activity = (MainActivity) getActivity();
		return activity.getContactsList();
	}

	private class MyInfoWindowAdapter implements InfoWindowAdapter {

		@Override
		public View getInfoContents(Marker marker) {
			View view = getActivity().getLayoutInflater().inflate(
					R.layout.marker_info_contents, null);
			TextView tvTitle = (TextView) view.findViewById(R.id.title);
			TextView tvSnippet = (TextView) view.findViewById(R.id.snippet);
			String title = marker.getTitle();
			String snippet = marker.getSnippet();
			snippet = snippet.replaceAll("[ 　]", "\n");
			tvTitle.setText(title);
			tvSnippet.setText(snippet);
			return view;
		}

		@Override
		public View getInfoWindow(Marker marker) {
			return null;
		}

	}

}
