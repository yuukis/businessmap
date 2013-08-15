package com.github.yuukis.businessmap;

import java.util.ArrayList;
import java.util.List;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.GroupMembership;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Data;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class ContactsListFragment extends ListFragment {

	private List<ContactsItem> mContactsList;
	private ContactsAdapter mContactsAdapter;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mContactsList = new ArrayList<ContactsItem>();
		mContactsAdapter = new ContactsAdapter();
		setListAdapter(mContactsAdapter);
	}

	public void loadContactsByGroupId(long gid) {

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
						String.valueOf(gid)},
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

		mContactsList.clear();
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

		mContactsAdapter.notifyDataSetChanged();
	}

	private static class ViewHolder {
		TextView textView1;
		TextView textView2;
	}

	private class ContactsAdapter extends BaseAdapter {

		@Override
		public int getCount() {
			return mContactsList.size();
		}

		@Override
		public Object getItem(int position) {
			return mContactsList.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView = getActivity().getLayoutInflater().inflate(
						android.R.layout.simple_list_item_2, null);
				holder = new ViewHolder();
				holder.textView1 = (TextView) convertView
						.findViewById(android.R.id.text1);
				holder.textView2 = (TextView) convertView
						.findViewById(android.R.id.text2);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			ContactsItem contact = (ContactsItem) getItem(position);
			holder.textView1.setText(contact.getName());
			holder.textView2.setText(contact.getAddress());

			return convertView;
		}

	}
}
