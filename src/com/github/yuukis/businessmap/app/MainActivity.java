package com.github.yuukis.businessmap.app;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.utils.CursorJoinerWithIntKey;
import com.github.yuukis.businessmap.utils.ContactsItemComparator;

import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.database.Cursor;
import android.view.Window;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	private static final String KEY_GROUP_LIST = "group_list";
	private static final String KEY_CONTACTS_LIST = "contacts_list";
	private static final String KEY_SELECTED_NAV_INDEX = "selected_nav_item";
	private static final String KEY_SHOW_LIST = "show_list";
	private static final int PROGRESS_MAX = 10000;

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private ContactsMapFragment mMapFragment;
	private ContactsListFragment mListFragment;
	private Handler mHandler = new Handler();
	private GeocodingThread mThread;

	@SuppressWarnings("unchecked")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_main);

		setProgressBarVisibility(false);

		FragmentManager fm = getFragmentManager();
		mMapFragment = (ContactsMapFragment) fm
				.findFragmentById(R.id.contacts_map);
		mListFragment = (ContactsListFragment) fm
				.findFragmentById(R.id.contacts_list);

		int selectedNavIndex = -1;
		if (savedInstanceState != null) {
			mGroupList = (List<ContactsGroup>) savedInstanceState
					.getSerializable(KEY_GROUP_LIST);
			mContactsList = (List<ContactsItem>) savedInstanceState
					.getSerializable(KEY_CONTACTS_LIST);
			selectedNavIndex = savedInstanceState.getInt(KEY_SELECTED_NAV_INDEX, -1);
			boolean showList = savedInstanceState.getBoolean(KEY_SHOW_LIST, false);
			mListFragment.setVisibility(showList);
		}
		if (mGroupList == null) {
			Cursor groupCursor = getContentResolver().query(
					Groups.CONTENT_URI,
					new String[] {
							Groups._ID,
							Groups.TITLE,
							Groups.ACCOUNT_NAME },
					Groups.DELETED + "=0",
					null,
					null);

			mGroupList = new ArrayList<ContactsGroup>();

			while (groupCursor.moveToNext()) {
				long _id = groupCursor.getLong(0);
				String title = groupCursor.getString(1);
				String accountName = groupCursor.getString(2);
				ContactsGroup group = new ContactsGroup(_id, title, accountName);
				mGroupList.add(group);
			}
		}
		if (mContactsList == null) {
			mContactsList = new ArrayList<ContactsItem>();
		}

		ArrayAdapter<ContactsGroup> adapter = new ArrayAdapter<ContactsGroup>(
				this, android.R.layout.simple_spinner_dropdown_item, mGroupList);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);
		if (selectedNavIndex >= 0) {
			actionBar.setSelectedNavigationItem(selectedNavIndex);
		}
	}

	@Override
	protected void onDestroy() {
		if (mThread != null) {
			mThread.halt();
		}
		super.onDestroy();
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		int index = getActionBar().getSelectedNavigationIndex();
		boolean showList = mListFragment.getVisibility();
		outState.putSerializable(KEY_GROUP_LIST, (Serializable) mGroupList);
		outState.putSerializable(KEY_CONTACTS_LIST, (Serializable) mContactsList);
		outState.putInt(KEY_SELECTED_NAV_INDEX, index);
		outState.putBoolean(KEY_SHOW_LIST, showList);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (mGroupList.size() <= itemPosition) {
			return false;
		}
		ContactsGroup group = mGroupList.get(itemPosition);
		loadContactsByGroupId(group.getId());
		return true;
	}

	public List<ContactsItem> getContactsList() {
		return mContactsList;
	}

	public void loadContactsByGroupId(long gid) {
		if (mThread != null) {
			mThread.halt();
		}

		Cursor groupCursor = getContentResolver().query(
				Data.CONTENT_URI,
				new String[]{
						GroupMembership.RAW_CONTACT_ID,
						GroupMembership.CONTACT_ID,
						GroupMembership.DISPLAY_NAME,
						GroupMembership.PHONETIC_NAME},
				Data.MIMETYPE + "=? AND " +
						GroupMembership.GROUP_ROW_ID + "=?",
				new String[] {
						GroupMembership.CONTENT_ITEM_TYPE,
						String.valueOf(gid)},
				Data.RAW_CONTACT_ID);

		Cursor postalCursor = getContentResolver().query(
				StructuredPostal.CONTENT_URI,
				new String[] {
						StructuredPostal.RAW_CONTACT_ID,
						StructuredPostal.FORMATTED_ADDRESS },
				null,
				null,
				StructuredPostal.RAW_CONTACT_ID);

		CursorJoinerWithIntKey joiner = new CursorJoinerWithIntKey(
				groupCursor, new String[] { Data.RAW_CONTACT_ID },
				postalCursor, new String[] { Data.RAW_CONTACT_ID });

		mContactsList.clear();
		mHandler.post(new Runnable() {
			@Override
			public void run() {
				mMapFragment.notifyDataSetChanged();
				mListFragment.notifyDataSetChanged();
			}
		});

		for (CursorJoinerWithIntKey.Result result : joiner) {
			long cid;
			String name, phonetic, address;

			switch (result) {
			case LEFT:
				cid = groupCursor.getLong(1);
				name = groupCursor.getString(2);
				phonetic = groupCursor.getString(3);
				address = null;
				break;

			case BOTH:
				cid = groupCursor.getLong(1);
				name = groupCursor.getString(2);
				phonetic = groupCursor.getString(3);
				address = postalCursor.getString(1);
				break;

			default:
				continue;
			}

			mContactsList.add(new ContactsItem(cid, name, phonetic, address));
		}

		groupCursor.close();
		postalCursor.close();

		Collections.sort(mContactsList, new ContactsItemComparator());

		mThread = new GeocodingThread();
		mThread.start();
	}

	private class GeocodingThread extends Thread {

		private boolean halt;

		public GeocodingThread() {
			halt = false;
		}

		@Override
		public void run() {
			findLatLng();
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					if (!halt) {
						mMapFragment.notifyDataSetChanged();
						mListFragment.notifyDataSetChanged();
					}
				}
			});
		}

		public void halt() {
			halt = true;
			interrupt();
		}

		private void findLatLng() {
			final GeocodingCacheDatabase db = new GeocodingCacheDatabase(
					MainActivity.this);
			final int listSize = mContactsList.size();
			for (int i = 0; i < listSize; i++) {
				if (halt) { return; }
				final int progress = i * PROGRESS_MAX / listSize;
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						setProgress(progress);
					}
				});

				ContactsItem contact = mContactsList.get(i);
				String address = contact.getAddress();
				if (address == null) {
					continue;
				}

				double[] latlng = db.get(address);
				if (latlng != null) {
					contact.setLat(latlng[0]);
					contact.setLng(latlng[1]);
					mContactsList.set(i, contact);
				} else {
					List<Address> list;
					try {
						list = new Geocoder(MainActivity.this,
								Locale.getDefault()).getFromLocationName(
								address, 1);
					} catch (IOException e) {
						continue;
					}
					if (list.size() == 0) {
						continue;
					}
					Address addr = list.get(0);
					double lat = addr.getLatitude();
					double lng = addr.getLongitude();
					contact.setLat(lat);
					contact.setLng(lng);
					db.put(address, new double[] { lat, lng });
				}
				if (halt) {
					db.close();
					return;
				}
				mContactsList.set(i, contact);
			}
			db.close();

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					setProgress(PROGRESS_MAX);
				}
			});
		}

	}
}
