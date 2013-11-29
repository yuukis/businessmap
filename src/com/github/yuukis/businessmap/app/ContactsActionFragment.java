/*
 * ContactsActionFragment.java
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

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsItem;
import com.github.yuukis.businessmap.util.ActionUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ContactsActionFragment extends DialogFragment implements
AdapterView.OnItemClickListener {

	private static final String TAG = "ContactsActionFragment";
	private static final String KEY_CONTACTS = "contacts";
	private static final int ID_SHOW_CONTACTS = 1;
	private static final int ID_DIRECTION = 2;
	private static final int ID_NAVIGATION = 3;
	private static final BindData[] ACTION_ITEMS = {
		new BindData(
				ID_SHOW_CONTACTS,
				R.drawable.ic_action_person,
				R.string.action_contacts_detail),
		new BindData(
				ID_DIRECTION,
				R.drawable.ic_action_directions,
				R.string.action_directions),
		new BindData(
				ID_NAVIGATION,
				R.drawable.ic_action_navigation,
				R.string.action_drive_navigation),
	};

	public static ContactsActionFragment newInstance(ContactsItem contact) {
		ContactsActionFragment fragment = new ContactsActionFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_CONTACTS, contact);
		fragment.setArguments(args);
		return fragment;
	}

	public static void showDialog(Activity activity, ContactsItem contact) {
		FragmentManager manager = activity.getFragmentManager();
		newInstance(contact).show(manager, TAG);
	}

	private ContactsItem mContact;

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		mContact = (ContactsItem) getArguments().getSerializable(KEY_CONTACTS);

		MenuAdapter adapter = new MenuAdapter(getActivity(),
				R.layout.gridview_contents, ACTION_ITEMS);
		int columns = getResources().getInteger(R.integer.gridview_columns);
		GridView gridView = new GridView(getActivity());
		gridView.setNumColumns(columns);
		gridView.setAdapter(adapter);
		gridView.setOnItemClickListener(this);
		String title = mContact.getName();

		return new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setView(gridView)
				.setNegativeButton(android.R.string.cancel, null)
				.create();
	}

	@Override
	public void onItemClick(AdapterView<?> parent, View view, int position,
			long id) {
		Context context = getActivity();
		BindData data = ACTION_ITEMS[position % ACTION_ITEMS.length];

		switch (data.itemId) {
		case ID_SHOW_CONTACTS:
			ActionUtils.doShowContact(context, mContact);
			break;

		case ID_DIRECTION:
			ActionUtils.doShowDirections(context, mContact);
			break;

		case ID_NAVIGATION:
			ActionUtils
					.doStartDriveNavigation(context, mContact);
		}
	}

	private static class BindData {
		int itemId;
		int iconId;
		int titleId;

		public BindData(int itemId, int iconId, int titleId) {
			this.itemId = itemId;
			this.iconId = iconId;
			this.titleId = titleId;
		}
	}

	private static class ViewHolder {
		ImageView imageView;
		TextView textView;
	}

	private class MenuAdapter extends ArrayAdapter<BindData> {

		private LayoutInflater inflater;
		private int layoutId;

		public MenuAdapter(Context context, int resource, BindData[] objects) {
			super(context, resource, objects);
			inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			layoutId = resource;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;

			if (convertView == null) {
				convertView =
						inflater.inflate(layoutId, parent, false);
				holder = new ViewHolder();
				holder.textView = (TextView) convertView
						.findViewById(android.R.id.title);
				holder.imageView = (ImageView) convertView
						.findViewById(android.R.id.icon);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}
			BindData data = getItem(position);
			holder.textView.setText(data.titleId);
			holder.imageView.setImageResource(data.iconId);

			return convertView;
		}
	}

}
