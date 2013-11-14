/*
 * MainActivity.java
 *
 * Copyright 2013 Yuuki Shimizu.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.github.yuukis.businessmap.app;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.data.GeocodingCacheDatabase;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.utils.CursorJoinerWithIntKey;
import com.github.yuukis.businessmap.utils.ContactsItemComparator;
import com.github.yuukis.businessmap.utils.GeocoderUtils;
import com.github.yuukis.businessmap.widget.GroupAdapter;

import android.os.Bundle;
import android.os.Handler;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.Groups;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	private static final String KEY_NAVIGATION_INDEX = "navigation_index";
	private static final String KEY_CONTACTSLIST = "contacts_list";
	private static final long ID_GROUP_ALL_CONTACTS = -1;

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private List<ContactsItem> mCurrentGroupContactsList;
	private Map<String, Double[]> mGeocodingResultCache;
	private ContactsMapFragment mMapFragment;
	private ContactsListFragment mListFragment;
	private ProgressDialog mProgressDialog;
	private Handler mHandler = new Handler();
	private GeocodingThread mThread;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_main);

		setProgressBarVisibility(false);

		FragmentManager fm = getFragmentManager();
		mMapFragment = (ContactsMapFragment) fm
				.findFragmentById(R.id.contacts_map);
		mListFragment = (ContactsListFragment) fm
				.findFragmentById(R.id.contacts_list);

		mGroupList = getContactsGroupList();
		mProgressDialog = new ProgressDialog(this);
		mProgressDialog.setCancelable(true);
		mProgressDialog.setCanceledOnTouchOutside(false);
		mProgressDialog.setTitle(R.string.title_geocoding);
		mProgressDialog.setMessage(getString(R.string.message_geocoding));
		mProgressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		mProgressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
			@Override
			public void onCancel(DialogInterface dialog) {
				if (mThread != null) {
					mThread.halt();
				}
			}
		});

		GroupAdapter adapter = new GroupAdapter(this, mGroupList);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);

		if (savedInstanceState != null) {
			actionBar.setSelectedNavigationItem(savedInstanceState
					.getInt(KEY_NAVIGATION_INDEX));
			mContactsList = (List<ContactsItem>) savedInstanceState
					.getSerializable(KEY_CONTACTSLIST);
		}
		mCurrentGroupContactsList = new ArrayList<ContactsItem>();
		mGeocodingResultCache = new HashMap<String, Double[]>();
		if (mContactsList == null) {
			mContactsList = new ArrayList<ContactsItem>();
			mThread = new GeocodingThread();
			mThread.start();
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
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_about:
			AboutDialogFragment.showDialog(this);
			return true;
		}
		return false;
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt(KEY_NAVIGATION_INDEX, getActionBar()
				.getSelectedNavigationIndex());
		outState.putSerializable(KEY_CONTACTSLIST, (Serializable) mContactsList);
		super.onSaveInstanceState(outState);
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		if (mGroupList.size() <= itemPosition) {
			return false;
		}
		ContactsGroup group = mGroupList.get(itemPosition);
		long groupId = group.getId();
		changeCurrentGroup(groupId);
		mMapFragment.notifyDataSetChanged();
		mListFragment.notifyDataSetChanged();

		return true;
	}

	public List<ContactsItem> getCurrentContactsList() {
		return mCurrentGroupContactsList;
	}

	public List<ContactsGroup> getContactsGroupList() {

		List<ContactsGroup> list = new ArrayList<ContactsGroup>();
		ContactsGroup all = new ContactsGroup(ID_GROUP_ALL_CONTACTS,
				getString(R.string.group_all_contacts), "");
		list.add(all);

		Cursor groupCursor = null;
		try {
			groupCursor = getContentResolver().query(
					Groups.CONTENT_URI,
					new String[] {
							Groups._ID,
							Groups.TITLE,
							Groups.ACCOUNT_NAME },
					Groups.DELETED + "=0",
					null,
					null);
			while (groupCursor.moveToNext()) {
				long _id = groupCursor.getLong(0);
				String title = groupCursor.getString(1);
				String accountName = groupCursor.getString(2);
				ContactsGroup group = new ContactsGroup(_id, title, accountName);
				list.add(group);
			}
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
			}
		}
		return list;
	}

	private void changeCurrentGroup(long groupId) {
		mCurrentGroupContactsList.clear();
		for (ContactsItem contact : mContactsList) {
			if (contact.getGroupId() == groupId) {
				mCurrentGroupContactsList.add(contact);
			}
		}
	}

	private void loadAllContacts() {
		mGeocodingResultCache.clear();

		List<ContactsItem> contactsList = new ArrayList<ContactsItem>();
		Cursor groupCursor = null;
		Cursor postalCursor = null;
		Cursor noteCursor = null;
		GeocodingCacheDatabase db = null;

		try {
			groupCursor = getContentResolver().query(
					Data.CONTENT_URI,
					new String[] {
							GroupMembership.RAW_CONTACT_ID,
							GroupMembership.CONTACT_ID,
							GroupMembership.DISPLAY_NAME,
							GroupMembership.PHONETIC_NAME,
							GroupMembership.GROUP_ROW_ID },
					Data.MIMETYPE + "=?",
					new String[] {
							GroupMembership.CONTENT_ITEM_TYPE },
					Data.RAW_CONTACT_ID);

			postalCursor = getContentResolver().query(
					StructuredPostal.CONTENT_URI,
					new String[] {
							StructuredPostal.RAW_CONTACT_ID,
							StructuredPostal.CONTACT_ID,
							StructuredPostal.DISPLAY_NAME,
							StructuredPostal.PHONETIC_NAME,
							StructuredPostal.FORMATTED_ADDRESS },
					null,
					null,
					StructuredPostal.RAW_CONTACT_ID);

			noteCursor = getContentResolver().query(
					Data.CONTENT_URI,
					new String[] {
							Note.RAW_CONTACT_ID,
							Note.NOTE },
					Data.MIMETYPE + "=?",
					new String[] {
							Note.CONTENT_ITEM_TYPE },
					Data.RAW_CONTACT_ID);
			HashMap<Long, String> noteMap = new HashMap<Long, String>();
			while (noteCursor.moveToNext()) {
				long rowId = noteCursor.getLong(0);
				String note = noteCursor.getString(1);
				noteMap.put(rowId, note);
			}

			CursorJoinerWithIntKey joiner = new CursorJoinerWithIntKey(
					groupCursor, new String[] { Data.RAW_CONTACT_ID },
					postalCursor, new String[] { Data.RAW_CONTACT_ID });

			db = new GeocodingCacheDatabase(this);
			long _rowId = -1, _cid = -1;
			String _name = null, _phonetic = null, _note = null;
			List<Long> _groupIds = new ArrayList<Long>();
			List<String> _address = new ArrayList<String>();

			for (CursorJoinerWithIntKey.Result result : joiner) {
				long rowId, cid, groupId;
				String name, phonetic, address, note;

				switch (result) {
				case LEFT:
					rowId = groupCursor.getLong(0);
					cid = groupCursor.getLong(1);
					name = groupCursor.getString(2);
					phonetic = groupCursor.getString(3);
					groupId = groupCursor.getLong(4);
					address = null;
					note = noteMap.get(rowId);
					break;

				case RIGHT:
					rowId = postalCursor.getLong(0);
					cid = postalCursor.getLong(1);
					name = postalCursor.getString(2);
					phonetic = postalCursor.getString(3);
					groupId = ID_GROUP_ALL_CONTACTS;
					address = postalCursor.getString(4);
					note = noteMap.get(rowId);
					break;

				case BOTH:
					rowId = groupCursor.getLong(0);
					cid = groupCursor.getLong(1);
					name = groupCursor.getString(2);
					phonetic = groupCursor.getString(3);
					groupId = groupCursor.getLong(4);
					address = postalCursor.getString(4);
					note = noteMap.get(rowId);
					break;

				default:
					continue;
				}

				if (_rowId != rowId) {
					for (long gid : _groupIds) {
						if (_address.isEmpty()) {
							contactsList.add(new ContactsItem(_cid, _name,
									_phonetic, gid, null, _note));
							continue;
						}
						for (String addr : _address) {
							ContactsItem contact = new ContactsItem(_cid,
									_name, _phonetic, gid, addr, _note);
							double[] latlng = db.get(addr);
							if (latlng != null && latlng.length == 2) {
								contact.setLat(latlng[0]);
								contact.setLng(latlng[1]);
							} else {
								if (!mGeocodingResultCache.containsKey(addr)) {
									mGeocodingResultCache.put(addr, null);
								}
							}
							contactsList.add(contact);
						}
					}
					_rowId = rowId;
					_cid = cid;
					_name = name;
					_phonetic = phonetic;
					_groupIds.clear();
					_groupIds.add(ID_GROUP_ALL_CONTACTS);
					_address.clear();
					_note = note;
				}

				if (_groupIds.indexOf(groupId) < 0) {
					_groupIds.add(groupId);
				}
				if (address != null && _address.indexOf(address) < 0) {
					_address.add(address);
				}
			}
			// FIXME: 冗長
			for (long gid : _groupIds) {
				if (_address.isEmpty()) {
					contactsList.add(new ContactsItem(_cid, _name, _phonetic,
							gid, null, _note));
					continue;
				}
				for (String addr : _address) {
					ContactsItem contact = new ContactsItem(_cid, _name,
							_phonetic, gid, addr, _note);
					double[] latlng = db.get(addr);
					if (latlng != null && latlng.length == 2) {
						contact.setLat(latlng[0]);
						contact.setLng(latlng[1]);
					} else {
						if (!mGeocodingResultCache.containsKey(addr)) {
							mGeocodingResultCache.put(addr, null);
						}
					}
					contactsList.add(contact);
				}
			}
		} finally {
			if (groupCursor != null) {
				groupCursor.close();
			}
			if (postalCursor != null) {
				postalCursor.close();
			}
			if (noteCursor != null) {
				noteCursor.close();
			}
			if (db != null) {
				db.close();
			}
		}
		Collections.sort(contactsList, new ContactsItemComparator());
		mContactsList = contactsList;
	}

	private class GeocodingThread extends Thread {

		private boolean halt;

		public GeocodingThread() {
			halt = false;
		}

		@Override
		public void run() {
			loadAllContacts();
			if (!mGeocodingResultCache.isEmpty()) {
				geocoding();
			}

			int index = getActionBar().getSelectedNavigationIndex();
			ContactsGroup group = mGroupList.get(index);
			long groupId = group.getId();
			changeCurrentGroup(groupId);
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

		private void geocoding() {
//			final Map<String, Double[]> map = mGeocodingResultCache;
			mProgressDialog.setMax(mGeocodingResultCache.size());
			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mProgressDialog.show();
				}
			});

			final GeocodingCacheDatabase db = new GeocodingCacheDatabase(
					MainActivity.this);
			int count = 0;
			try {
				for (Iterator<Entry<String, Double[]>> it = mGeocodingResultCache
						.entrySet().iterator(); it.hasNext();) {
					Entry<String, Double[]> entry = it.next();
					String address = entry.getKey();

					Double[] latlng = GeocoderUtils.getFromLocationName(
							MainActivity.this, address);
					db.put(address, latlng);
					entry.setValue(latlng);

					count++;
					final int progress = count;
					mHandler.post(new Runnable() {
						@Override
						public void run() {
							mProgressDialog.setProgress(progress);
						}
					});

					if (halt) {
						return;
					}
				}
			} catch (IOException e) {
				mHandler.post(new Runnable() {
					@Override
					public void run() {
						mProgressDialog.dismiss();
						new AlertDialog.Builder(MainActivity.this)
								.setTitle(R.string.title_geocoding_ioerror)
								.setMessage(R.string.message_geocoding_ioerror)
								.setPositiveButton(android.R.string.ok, null)
								.show();
					}
				});
				return;
			} finally {
				db.close();
			}

			for (int j = 0; j < mContactsList.size(); j++) {
				ContactsItem contact = mContactsList.get(j);
				String address = contact.getAddress();
				if (address == null) {
					continue;
				}
				if (contact.getLat() != null && contact.getLng() != null) {
					continue;
				}
				if (!mGeocodingResultCache.containsKey(address)) {
					continue;
				}

				Double[] latlng = mGeocodingResultCache.get(address);
				if (latlng != null && latlng.length == 2) {
					contact.setLat(latlng[0]);
					contact.setLng(latlng[1]);
					mContactsList.set(j, contact);
				}
			}

			mHandler.post(new Runnable() {
				@Override
				public void run() {
					mProgressDialog.setProgress(mProgressDialog.getMax());
					mProgressDialog.dismiss();
				}
			});
		}

	}
}
