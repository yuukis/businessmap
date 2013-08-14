package com.github.yuukis.businessmap.app;

import java.util.ArrayList;
import java.util.List;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;

import android.os.Bundle;
import android.provider.ContactsContract.Groups;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.database.Cursor;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;

public class MainActivity extends Activity implements
		ActionBar.OnNavigationListener {

	Fragment mMapFragment, mListFragment;
	List<ContactsGroup> mGroupList;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Cursor groupCursor = getContentResolver().query(
				Groups.CONTENT_URI,
				new String[] {
						Groups._ID,
						Groups.TITLE,
						Groups.ACCOUNT_NAME },
				Groups.DELETED + "=0",
				null,
				null);

		FragmentManager fm = getFragmentManager();
		mMapFragment = fm.findFragmentById(R.id.map);
		mListFragment = fm.findFragmentById(R.id.contacts_list);
		mGroupList = new ArrayList<ContactsGroup>();

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
		long gid = group.getId();
		((ContactsListFragment) mListFragment).loadContactsByGroupId(gid);
		return true;
	}

}
