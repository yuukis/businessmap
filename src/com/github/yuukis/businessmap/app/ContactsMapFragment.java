package com.github.yuukis.businessmap.app;

import java.util.List;
import java.util.Locale;

import android.app.AlertDialog;
import android.content.ContentUris;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Contacts;
import android.util.SparseArray;
import android.view.View;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ContactsMapFragment extends MapFragment implements
		GoogleMap.OnInfoWindowClickListener {

	private SparseArray<ContactsItem> mMarkerHashMap;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mMarkerHashMap = new SparseArray<ContactsItem>();
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		GoogleMap map = getMap();
		map.setInfoWindowAdapter(new MyInfoWindowAdapter());
		map.setOnInfoWindowClickListener(this);
		map.setIndoorEnabled(true);
		map.setMyLocationEnabled(true);
	}

	public void notifyDataSetChanged() {
		getMap().clear();
		mMarkerHashMap.clear();
		for (ContactsItem contact : getContactsList()) {
			if (contact.getLat() == null || contact.getLng() == null) {
				continue;
			}
			LatLng latLng = new LatLng(contact.getLat(), contact.getLng());
			Marker marker = getMap().addMarker(new MarkerOptions()
					.position(latLng)
					.title(contact.getName())
					.snippet(contact.getDisplayAddress()));
			mMarkerHashMap.put(marker.hashCode(), contact);
		}
	}

	@Override
	public void onInfoWindowClick(Marker marker) {
		final ContactsItem contact = mMarkerHashMap.get(marker.hashCode());
		if (contact == null) {
			return;
		}
		String title = contact.getName();
		final String[] items = new String[] {
				"Show contacts",
				"Show directions",
				"Start drive navigation"
		};
		new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							doShowContact(contact);
							break;
						case 1:
							doShowDirections(contact);
							break;
						case 2:
							doStartDriveNavigation(contact);
						}
					}
				})
				.show();
	}

	private void doShowContact(ContactsItem contact) {
		Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
				contact.getCID());
		Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
		startActivity(intent);
	}

	private void doShowDirections(ContactsItem contact) {
		Uri uri = Uri.parse(String.format(Locale.getDefault(),
				"http://maps.google.com/maps?saddr=&daddr=%f,%f",
				contact.getLat(), contact.getLng()));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		startActivity(intent);
	}

	private void doStartDriveNavigation(ContactsItem contact) {
		Uri uri = Uri.parse(String.format(Locale.getDefault(),
				"google.navigation:///?ll=%f,%f&q=%s",
				contact.getLat(), contact.getLng(), contact.getName()));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.setClassName("com.google.android.apps.maps",
				"com.google.android.maps.driveabout.app.NavigationActivity");
		startActivity(intent);
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
