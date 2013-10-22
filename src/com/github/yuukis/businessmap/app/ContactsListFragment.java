package com.github.yuukis.businessmap.app;

import java.util.ArrayList;
import java.util.List;

import com.github.yuukis.businessmap.model.ContactsItem;

import android.app.AlertDialog;
import android.app.ListFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.SearchView.OnQueryTextListener;
import android.widget.TextView;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.utils.ActionUtils;
import com.github.yuukis.businessmap.utils.StringJUtils;
import com.slidinglayer.SlidingLayer;

public class ContactsListFragment extends ListFragment implements
		OnQueryTextListener {

	private ContactsAdapter mContactsAdapter;
	private SearchView mSearchView;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {
		setHasOptionsMenu(true);
		return inflater.inflate(R.layout.fragment_contscts_list, null);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mSearchView = (SearchView) getView().findViewById(R.id.searchview);
		mSearchView.setIconifiedByDefault(false);
		mSearchView.setOnQueryTextListener(this);
		mSearchView.setSubmitButtonEnabled(false);
		mContactsAdapter = new ContactsAdapter();
		setListAdapter(mContactsAdapter);
		setEmptyText(getString(R.string.message_no_contacts));
		getListView().setTextFilterEnabled(true);
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

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		mSearchView.clearFocus();
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
		final String[] items = new String[] { getString(R.string.action_contacts_detail) };
		new AlertDialog.Builder(getActivity()).setTitle(title)
				.setItems(items, new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						switch (which) {
						case 0:
							ActionUtils.doShowContact(context, contact);
							break;
						}
					}
				}).show();
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		ListView listView = getListView();
		if (TextUtils.isEmpty(newText)) {
			listView.clearTextFilter();
		} else {
			listView.setFilterText(newText.toString());
		}
		return true;
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		mSearchView.clearFocus();
		return false;
	}

	public void notifyDataSetChanged() {
		mSearchView.clearFocus();
		mContactsAdapter.notifyDataSetChanged();
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
			mSearchView.clearFocus();
			listContainer.closeLayer(true);
		}
		getFragmentManager().invalidateOptionsMenu();
	}

	@Override
	public void setEmptyText(CharSequence text) {
		TextView tv = (TextView) getListView().getEmptyView();
		tv.setText(text);
	}

	private List<ContactsItem> getContactsList() {
		MainActivity activity = (MainActivity) getActivity();
		return activity.getCurrentContactsList();
	}

	private static class ViewHolder {
		TextView textView1;
		TextView textView2;
	}

	private class ContactsAdapter extends BaseAdapter implements Filterable {

		private List<ContactsItem> mFilterResultList = null;

		@Override
		public int getCount() {
			List<ContactsItem> list;
			if (mFilterResultList == null) {
				list = getContactsList();
			} else {
				list = mFilterResultList;
			}
			if (list == null) {
				return 0;
			}
			return list.size();
		}

		@Override
		public Object getItem(int position) {
			List<ContactsItem> list;
			if (mFilterResultList == null) {
				list = getContactsList();
			} else {
				list = mFilterResultList;
			}
			if (list == null) {
				return null;
			}
			return list.get(position);
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
				address = getString(R.string.message_no_data);
			}
			holder.textView1.setText(name);
			holder.textView2.setText(address);

			return convertView;
		}

		@Override
		public Filter getFilter() {
			Filter filter = new Filter() {

				@Override
				protected FilterResults performFiltering(CharSequence constraint) {
					FilterResults results = new FilterResults();
					List<ContactsItem> contactsList = getContactsList();
					if (TextUtils.isEmpty(constraint)) {
						results.values = null;
						results.count = 0;
					} else {
						ArrayList<ContactsItem> filterResultData = new ArrayList<ContactsItem>();
						for (ContactsItem contacts : contactsList) {
							String query = constraint.toString();
							query = StringJUtils.convertToKatakana(query);
							
							String name = contacts.getName();
							if (name != null) {
								name = StringJUtils.convertToKatakana(name);
								if (name.indexOf(query) >= 0) {
									filterResultData.add(contacts);
									continue;
								}
							}
							
							String phonetic = contacts.getPhontic();
							if (phonetic != null) {
								phonetic = StringJUtils.convertToKatakana(phonetic);
								if (phonetic.indexOf(query) >= 0) {
									filterResultData.add(contacts);
									continue;
								}
							}
						}
						results.values = filterResultData;
						results.count = filterResultData.size();
					}
					return results;
				}

				@SuppressWarnings("unchecked")
				@Override
				protected void publishResults(CharSequence constraint,
						FilterResults results) {
					mFilterResultList = (List<ContactsItem>) results.values;
					notifyDataSetChanged();
				}
			};
			return filter;
		}

	}
}
