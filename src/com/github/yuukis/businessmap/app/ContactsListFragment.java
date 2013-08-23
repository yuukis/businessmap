package com.github.yuukis.businessmap.app;

import java.util.List;

import com.github.yuukis.businessmap.model.ContactsItem;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.utils.ActionUtils;
import com.slidinglayer.SlidingLayer;

public class ContactsListFragment extends ListFragment {

	private ContactsAdapter mContactsAdapter;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		return super.onCreateView(inflater, container, savedInstanceState);
	}

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
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		inflater.inflate(R.menu.contacts_list, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_contacts:
			if (getVisibility()) {
				setVisibility(false);
			} else {
				setVisibility(true);
			}
			return true;
		}
		return false;
	}

	public boolean getVisibility() {
		MainActivity activity = (MainActivity) getActivity();
		SlidingLayer listContainer = (SlidingLayer) activity
				.findViewById(R.id.list_container);
		return listContainer.isOpened();
	}

	public void setVisibility(boolean visible) {
		MainActivity activity = (MainActivity) getActivity();
		SlidingLayer listContainer = (SlidingLayer) activity
				.findViewById(R.id.list_container);
		if (visible) {
			listContainer.openLayer(true);
		} else {
			listContainer.closeLayer(true);
		}
		getFragmentManager().invalidateOptionsMenu();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		final ContactsItem contact = (ContactsItem) mContactsAdapter
				.getItem(position);
		ContactsMapFragment mapFragment = (ContactsMapFragment) getFragmentManager()
				.findFragmentById(R.id.contacts_map);
		if (mapFragment != null) {
			boolean result = mapFragment.showMarkerInfoWindow(contact);
			if (result) {
				setVisibility(false);
				return;
			}
		}

		final Context context = getActivity();
		String title = contact.getName();
		final String[] items = new String[] {
				getString(R.string.action_contacts_detail)
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
						}
					}
				})
				.show();
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
			String name = contact.getName();
			String address = contact.getAddress();
			if (address == null) {
				address = getString(R.string.message_no_address);
			}
			holder.textView1.setText(name);
			holder.textView2.setText(address);

			return convertView;
		}

	}
}
