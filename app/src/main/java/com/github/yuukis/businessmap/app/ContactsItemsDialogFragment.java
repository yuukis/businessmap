/*
 * ContactsListFragment.java
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
import java.util.List;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsItem;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;

public class ContactsItemsDialogFragment extends DialogFragment
		implements DialogInterface.OnClickListener {

	public interface OnSelectListener {
		public void onContactsSelected(ContactsItem contacts);
	}

	private static final String KEY_CONTACTS_ITEMS = "contacts_items";
	private static final String TAG = "ContactsGroupDialogFragment";

	private OnSelectListener mListener = null;
	private List<ContactsItem> mContactsItems;

	public static ContactsItemsDialogFragment newInstance(List<ContactsItem> contactsList) {
		ContactsItemsDialogFragment dialogFragment = new ContactsItemsDialogFragment();
		Bundle args = new Bundle();
		args.putSerializable(KEY_CONTACTS_ITEMS, (Serializable) contactsList);
		dialogFragment.setArguments(args);
		return dialogFragment;
	}

	public static void showDialog(FragmentActivity activity, List<ContactsItem> contactsList) {
		FragmentManager manager = activity.getSupportFragmentManager();
		newInstance(contactsList).show(manager, TAG);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnSelectListener) {
			mListener = (OnSelectListener) activity;
		}
		Bundle args = getArguments();
		if (args != null && args.containsKey(KEY_CONTACTS_ITEMS)) {
			mContactsItems = (List<ContactsItem>) args.get(KEY_CONTACTS_ITEMS);
		}
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		CharSequence[] items = getContactsItemsTitleArray();
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_select_contacts)
				.setItems(items, this)
				.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (mListener != null) {
			ContactsItem contacts = null;
			if (which != DialogInterface.BUTTON_NEGATIVE) {
				contacts = mContactsItems.get(which);
			}
			mListener.onContactsSelected(contacts);
		}
	}

	private CharSequence[] getContactsItemsTitleArray() {
		List<ContactsItem> contactsList = mContactsItems;
		int size = contactsList.size();
		CharSequence[] titleArray = new CharSequence[size];
		for (int i=0; i<size; i++) {
			ContactsItem contacts = contactsList.get(i);
			String title = contacts.getName();
			titleArray[i] = title;
		}
		return titleArray;
	}
}
