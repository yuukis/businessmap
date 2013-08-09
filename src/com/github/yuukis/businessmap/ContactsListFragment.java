package com.github.yuukis.businessmap;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

public class ContactsListFragment extends ListFragment {

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		Context context = getActivity();

		Cursor groupCursor = context.getContentResolver().query(
				Data.CONTENT_URI,
				new String[]{
						GroupMembership.RAW_CONTACT_ID,
						GroupMembership.DISPLAY_NAME },
				Data.MIMETYPE + "=? AND " +
						GroupMembership.GROUP_ROW_ID + "=?",
				new String[] {
						GroupMembership.CONTENT_ITEM_TYPE,
						String.valueOf(6)},
				Data.RAW_CONTACT_ID);

		Cursor postalCursor = context.getContentResolver().query(
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

		List<Map<String, String>> list = new ArrayList<Map<String, String>>();
		final String K = "key";
		final String V = "value";
		for (CursorJoinerWithIntKey.Result result : joiner) {
			Map<String,String> map = new HashMap<String, String>();
			String name, address;

			switch (result) {
			case LEFT:
				name = groupCursor.getString(1);
				address = "(未登録)";
				break;

			case BOTH:
				name = groupCursor.getString(1);
				address = postalCursor.getString(1);
				break;

			default:
				continue;
			}

			map.put(K, name);
			map.put(V, address);
			list.add(map);
		}

		groupCursor.close();
		postalCursor.close();

		ListAdapter adapter = new SimpleAdapter(
				context,
				list,
				android.R.layout.simple_list_item_2,
				new String[] { K, V },
				new int[] { android.R.id.text1, android.R.id.text2 });

		setListAdapter(adapter);
	}

}
