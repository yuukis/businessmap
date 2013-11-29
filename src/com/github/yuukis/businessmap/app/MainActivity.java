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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.task.ContactsAsyncTask;
import com.github.yuukis.businessmap.widget.GroupAdapter;

import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener, ContactsAsyncTask.Callback {

	private static final String KEY_NAVIGATION_INDEX = "navigation_index";
	private static final String KEY_CONTACTSLIST = "contacts_list";

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private List<ContactsItem> mCurrentGroupContactsList;
	private ContactsMapFragment mMapFragment;
	private ContactsListFragment mListFragment;
	private ContactsAsyncTask mContactsTask;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_PROGRESS);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_main);
		setProgressBarVisibility(false);
		initialize(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		if (mContactsTask != null) {
			mContactsTask.cancel(true);
			mContactsTask = null;
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
		int navigationIndex = getActionBar().getSelectedNavigationIndex();
		outState.putInt(KEY_NAVIGATION_INDEX, navigationIndex);
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

	@Override
	public void onContactsLoaded(List<ContactsItem> contactsList) {
		mContactsList = contactsList;
		int index = getActionBar().getSelectedNavigationIndex();
		ContactsGroup group = mGroupList.get(index);
		long groupId = group.getId();
		changeCurrentGroup(groupId);
		mMapFragment.notifyDataSetChanged();
		mListFragment.notifyDataSetChanged();
	}

	public List<ContactsItem> getCurrentContactsList() {
		return mCurrentGroupContactsList;
	}

	@SuppressWarnings("unchecked")
	private void initialize(Bundle savedInstanceState) {
		FragmentManager fm = getFragmentManager();
		mMapFragment = (ContactsMapFragment) fm
				.findFragmentById(R.id.contacts_map);
		mListFragment = (ContactsListFragment) fm
				.findFragmentById(R.id.contacts_list);
		mGroupList = getContactsGroupList();

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
		if (mContactsList == null) {
			mContactsList = new ArrayList<ContactsItem>();
			mContactsTask = new ContactsAsyncTask(this, this);
			mContactsTask.execute();
		}
	}

	private List<ContactsGroup> getContactsGroupList() {
		List<ContactsGroup> list = new ArrayList<ContactsGroup>();
		ContactsGroup all = new ContactsGroup(ContactsGroup.ID_GROUP_ALL_CONTACTS,
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
}
