package com.github.yuukis.businessmap.app;

import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.GoogleMap.InfoWindowAdapter;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class ContactsMapFragment extends MapFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		getMap().setInfoWindowAdapter(new MyInfoWindowAdapter());
	}

	public void notifyDataSetChanged() {
		getMap().clear();
		for (ContactsItem contact : getContactsList()) {
			if (contact.getLat() == null || contact.getLng() == null) {
				continue;
			}
			LatLng latLng = new LatLng(contact.getLat(), contact.getLng());
			getMap().addMarker(new MarkerOptions()
					.position(latLng)
					.title(contact.getName())
					.snippet(contact.getDisplayAddress()));
		}
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
