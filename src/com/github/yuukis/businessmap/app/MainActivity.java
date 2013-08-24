package com.github.yuukis.businessmap.app;

import java.io.IOException;
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

	private static final int PROGRESS_MAX = 10000;

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private ContactsMapFragment mMapFragment;
	private ContactsListFragment mListFragment;
	private Handler mHandler = new Handler();
	private GeocodingThread mThread;

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

		mGroupList = getContactsGroupList();
		mContactsList = new ArrayList<ContactsItem>();
		loadAllContacts();

		ArrayAdapter<ContactsGroup> adapter = new ArrayAdapter<ContactsGroup>(
				this, android.R.layout.simple_spinner_dropdown_item, mGroupList);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);
	}

	@Override
	protected void onDestroy() {
		if (mThread != null) {
			mThread.halt();
		}
		super.onDestroy();
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (mGroupList.size() <= itemPosition) {
			return false;
		}
		ContactsGroup group = mGroupList.get(itemPosition);
		// TODO: グループの切り替え
		//loadContactsByGroupId(group.getId());
		return true;
	}

	public List<ContactsItem> getContactsList() {
		return mContactsList;
	}

	public List<ContactsGroup> getContactsGroupList() {

		Cursor groupCursor = getContentResolver().query(
				Groups.CONTENT_URI,
				new String[] {
						Groups._ID,
						Groups.TITLE,
						Groups.ACCOUNT_NAME },
				Groups.DELETED + "=0",
				null,
				null);

		List<ContactsGroup> list = new ArrayList<ContactsGroup>();
		while (groupCursor.moveToNext()) {
			long _id = groupCursor.getLong(0);
			String title = groupCursor.getString(1);
			String accountName = groupCursor.getString(2);
			ContactsGroup group = new ContactsGroup(_id, title, accountName);
			list.add(group);
		}
		return list;
	}

	public void loadAllContacts() {
		if (mThread != null) {
			mThread.halt();
		}

		Cursor groupCursor = getContentResolver().query(
				Data.CONTENT_URI,
				new String[]{
						GroupMembership.RAW_CONTACT_ID,
						GroupMembership.CONTACT_ID,
						GroupMembership.DISPLAY_NAME,
						GroupMembership.PHONETIC_NAME,
						GroupMembership.GROUP_ROW_ID},
				Data.MIMETYPE + "=?",
				new String[] {
						GroupMembership.CONTENT_ITEM_TYPE},
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
			int groupId;

			switch (result) {
			case LEFT:
				cid = groupCursor.getLong(1);
				name = groupCursor.getString(2);
				phonetic = groupCursor.getString(3);
				groupId = groupCursor.getInt(4);
				address = null;
				break;

			case BOTH:
				cid = groupCursor.getLong(1);
				name = groupCursor.getString(2);
				phonetic = groupCursor.getString(3);
				groupId = groupCursor.getInt(4);
				address = postalCursor.getString(1);
				break;

			default:
				continue;
			}

			mContactsList.add(new ContactsItem(cid, name, phonetic, groupId, address));
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
