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
import com.github.yuukis.businessmap.util.ContactUtils;
import com.github.yuukis.businessmap.widget.GroupAdapter;

import android.os.Bundle;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener, ContactsTaskFragment.TaskCallback,
		ProgressDialogFragment.ProgressDialogFragmentListener,
		ContactsItemsDialogFragment.OnSelectListener {

	public static final String KEY_CONTACTS_GROUP_ID = "contacts_group_id";
	private static final String KEY_NAVIGATION_INDEX = "navigation_index";
	private static final String KEY_CONTACTS_LIST = "contacts_list";

	private List<ContactsGroup> mGroupList;
	private List<ContactsItem> mContactsList;
	private List<ContactsItem> mCurrentGroupContactsList;
	private ContactsMapFragment mMapFragment;
	private ContactsListFragment mListFragment;
	private ContactsTaskFragment mTaskFragment;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		getWindow().setSoftInputMode(
				WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
		setContentView(R.layout.activity_main);
		initialize(savedInstanceState);
	}

	@Override
	protected void onStart() {
		super.onStart();
		if (mContactsList == null) {
			mContactsList = new ArrayList<ContactsItem>();
			if (!mTaskFragment.isRunning()) {
				mTaskFragment.start();
			}
		}
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
		outState.putSerializable(KEY_CONTACTS_LIST, (Serializable) mContactsList);
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

	@Override
	public void onContactsSelected(ContactsItem contacts) {
		if (contacts == null) {
			return;
		}
		Toast.makeText(this, contacts.getName(), Toast.LENGTH_SHORT).show();
	}

	@Override
	public void onProgressCancelled() {
		mTaskFragment.cancel();
	}

	@Override
	public boolean dispatchKeyEvent(KeyEvent event) {
		if (event.getAction() == KeyEvent.ACTION_DOWN) {
			switch (event.getKeyCode()) {
			case KeyEvent.KEYCODE_BACK:
				// バックキーを押下
				ContactsListFragment listFragment = (ContactsListFragment) getFragmentManager()
						.findFragmentById(R.id.contacts_list);
				if (listFragment != null && listFragment.getVisibility()) {
					// 連絡先一覧が表示されている場合は、連絡先レイヤーを閉じる
					listFragment.setVisibility(false);
					return true;
				}
			}
		}
		return super.dispatchKeyEvent(event);
	}

	public List<ContactsItem> getCurrentContactsList() {
		return mCurrentGroupContactsList;
	}

	@SuppressWarnings("unchecked")
	private void initialize(Bundle savedInstanceState) {
		Bundle args = getIntent().getExtras();

		FragmentManager fm = getFragmentManager();
		mMapFragment = (ContactsMapFragment) fm.findFragmentById(R.id.contacts_map);
		mListFragment = (ContactsListFragment) fm.findFragmentById(R.id.contacts_list);
		mTaskFragment = (ContactsTaskFragment) fm.findFragmentById(R.id.contacts_task);
		mGroupList = ContactUtils.getContactsGroupList(this);

		GroupAdapter adapter = new GroupAdapter(this, mGroupList);
		ActionBar actionBar = getActionBar();
		actionBar.setDisplayShowTitleEnabled(false);
		actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		actionBar.setListNavigationCallbacks(adapter, this);

		int navigationIndex = 0;
		if (savedInstanceState != null) {
			navigationIndex = savedInstanceState.getInt(KEY_NAVIGATION_INDEX);
			mContactsList = (List<ContactsItem>) savedInstanceState
					.getSerializable(KEY_CONTACTS_LIST);
		} else if (args != null) {
			if (args.containsKey(KEY_CONTACTS_GROUP_ID)) {
				long groupId = args.getLong(KEY_CONTACTS_GROUP_ID);
				for (int i = 0; i < mGroupList.size(); i++) {
					ContactsGroup contactsGroup = mGroupList.get(i);
					if (groupId == contactsGroup.getId()) {
						navigationIndex = i;
						break;
					}
				}
			}
		}
		actionBar.setSelectedNavigationItem(navigationIndex);
		mCurrentGroupContactsList = new ArrayList<ContactsItem>();
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
