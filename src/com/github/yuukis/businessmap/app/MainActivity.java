package com.github.yuukis.businessmap.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.utils.CursorJoinerWithIntKey;

import com.google.android.gms.maps.MapFragment;

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
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.ArrayAdapter;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	private static final int PROGRESS_MAX = 10000;

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private MapFragment mMapFragment;
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
		mMapFragment = (MapFragment) fm.findFragmentById(R.id.map);
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
	protected void onDestroy() {
		if (mThread != null) {
			mThread.halt();
		}
		super.onDestroy();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {

		switch (item.getItemId()) {
		case R.id.action_contacts:
			View listContainer = findViewById(R.id.list_container);
			if (listContainer.getVisibility() == View.VISIBLE) {
				listContainer.setVisibility(View.INVISIBLE);
				item.setIcon(R.drawable.ic_action_list);
			} else {
				listContainer.setVisibility(View.VISIBLE);
				item.setIcon(R.drawable.ic_action_list_on);
			}
			return true;
		}
		return false;
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
		if (mThread != null) {
			mThread.halt();
		}

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

		mThread = new GeocodingThread();
		mThread.start();
	}

	public List<ContactsItem> getContactsList() {
		return mContactsList;
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
				try {
					List<Address> list = new Geocoder(MainActivity.this,
							Locale.getDefault())
							.getFromLocationName(address, 1);
					if (halt) {
						return;
					}
					if (list.size() > 0) {
						Address addr = list.get(0);
						contact.setLat(addr.getLatitude());
						contact.setLng(addr.getLongitude());
						mContactsList.set(i, contact);
					}
				} catch (IOException e) {
				}
			}
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					setProgress(PROGRESS_MAX);
				}
			});
		}

	}
}
