package com.github.yuukis.businessmap.app;

import java.util.List;

import com.github.yuukis.businessmap.model.ContactsItem;

import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;

public class ContactsListFragment extends ListFragment {

	private ContactsAdapter mContactsAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mContactsAdapter = new ContactsAdapter();
		setListAdapter(mContactsAdapter);
	}

	public void notifyDataSetChanged() {
		mContactsAdapter.notifyDataSetChanged();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		ContactsItem contact = (ContactsItem) mContactsAdapter
				.getItem(position);
		ContactsMapFragment mapFragment = (ContactsMapFragment) getFragmentManager()
				.findFragmentById(R.id.contacts_map);
		if (mapFragment != null) {
			mapFragment.showMarkerInfoWindow(contact);
		}
	}

	private List<ContactsItem> getContactsList() {
		MainActivity activity = (MainActivity) getActivity();
		return activity.getContactsList();
	}

	private static class ViewHolder {
		TextView textView1;
		TextView textView2;
	}

	private class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return getContactsList().size();
		}

		@Override
		public Object getItem(int position) {
			return getContactsList().get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						android.R.layout.simple_list_item_2, null);
				holder = new ViewHolder();
				holder.textView1 = (TextView) convertView
						.findViewById(android.R.id.text1);
				holder.textView2 = (TextView) convertView
						.findViewById(android.R.id.text2);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ContactsItem contact = (ContactsItem) getItem(position);
			holder.textView1.setText(contact.getName());
			holder.textView2.setText(contact.toString());

			return convertView;
		}

	}
}
