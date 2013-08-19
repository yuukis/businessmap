package com.github.yuukis.businessmap;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
import android.view.Menu;
import android.view.Window;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	private static final int PROGRESS_MAX = 10000;

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private ContactsListFragment mListFragment;
	private Handler mHandler = new Handler();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		setContentView(R.layout.activity_main);

		setProgressBarVisibility(false);

		FragmentManager fm = getFragmentManager();
		mListFragment = (ContactsListFragment) fm
				.findFragmentById(R.id.contacts_list);

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
		mContactsList = new ArrayList<ContactsItem>();

		while (groupCursor.moveToNext()) {
			long _id = groupCursor.getLong(0);
			String title = groupCursor.getString(1);
			String accountName = groupCursor.getString(2);
			ContactsGroup group = new ContactsGroup(_id, title, accountName);
			mGroupList.add(group);
		}

		ArrayAdapter<ContactsGroup> adapter = new ArrayAdapter<ContactsGroup>(
				this, android.R.layout.simple_spinner_dropdown_item, mGroupList);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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

	public void loadContactsByGroupId(long gid) {
		Cursor groupCursor = getContentResolver().query(
				Data.CONTENT_URI,
				new String[]{
						GroupMembership.RAW_CONTACT_ID,
						GroupMembership.DISPLAY_NAME },
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
				mListFragment.notifyDataSetChanged();
			}
		});

		for (CursorJoinerWithIntKey.Result result : joiner) {
			String name, address;

			switch (result) {
			case LEFT:
				name = groupCursor.getString(1);
				address = null;
				break;

			case BOTH:
				name = groupCursor.getString(1);
				address = postalCursor.getString(1);
				break;

			default:
				continue;
			}

			mContactsList.add(new ContactsItem(name, address));
		}

		groupCursor.close();
		postalCursor.close();

		new Thread(new Runnable() {
			@Override
			public void run() {
				findLatLng();
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mListFragment.notifyDataSetChanged();
					}
				});
			}
		}).start();
	}

	public List<ContactsItem> getContactsList() {
		return mContactsList;
	}

	private void findLatLng() {
		final int listSize = mContactsList.size();
		GeocodingCacheDatabase db = new GeocodingCacheDatabase(this);
		for (int i = 0; i < listSize; i++) {
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
					list = new Geocoder(this, Locale.getDefault())
							.getFromLocationName(address, 1);
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
