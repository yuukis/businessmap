package com.github.yuukis.businessmap.util;

import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.provider.ContactsContract.Groups;
import android.support.v4.content.ContextCompat;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;

public class ContactUtils {

	public static List<ContactsGroup> getContactsGroupList(Context context) {
		// Permission check
		if (ContextCompat.checkSelfPermission(context,
				Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
			return new ArrayList<>();
		}

		List<ContactsGroup> list = new ArrayList<ContactsGroup>();
		ContactsGroup all = new ContactsGroup(
				ContactsGroup.ID_GROUP_ALL_CONTACTS,
				context.getString(R.string.group_all_contacts), "");
		list.add(all);

		Cursor groupCursor = null;
		try {
			groupCursor = context.getContentResolver().query(
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
}
