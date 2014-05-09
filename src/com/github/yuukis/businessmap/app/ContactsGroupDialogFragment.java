/*
 * ContactsGroupDialogFragment.java
 *
 * Copyright 2014 Yuuki Shimizu.
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

import java.util.List;

import com.actionbarsherlock.app.SherlockDialogFragment;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;
import com.github.yuukis.businessmap.util.ContactUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

public class ContactsGroupDialogFragment extends SherlockDialogFragment
		implements DialogInterface.OnClickListener {

	public interface OnSelectListener {
		public void onContactsGroupSelected(ContactsGroup group);
	}

	private static final String TAG = "ContactsGroupDialogFragment";

	private OnSelectListener mListener = null;
	private List<ContactsGroup> mGroupList;

	public static ContactsGroupDialogFragment newInstance() {
		return new ContactsGroupDialogFragment();
	}

	public static void showDialog(SherlockFragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		newInstance().show(manager, TAG);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnSelectListener) {
			mListener = (OnSelectListener) activity;
		}
		mGroupList = ContactUtils.getContactsGroupList(activity);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		CharSequence[] items = getContactsGroupTitleArray();
		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_select_group)
				.setItems(items, this)
				.setNegativeButton(android.R.string.cancel, this)
				.create();
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (mListener != null) {
			ContactsGroup group = null;
			if (which != DialogInterface.BUTTON_NEGATIVE) {
				group = mGroupList.get(which);
			}
			mListener.onContactsGroupSelected(group);
		}
	}

	private CharSequence[] getContactsGroupTitleArray() {
		List<ContactsGroup> groupList = mGroupList;
		int size = groupList.size();
		CharSequence[] titleArray = new CharSequence[size];
		for (int i=0; i<size; i++) {
			ContactsGroup group = groupList.get(i);
			String title = group.getTitle();
			titleArray[i] = title;
		}
		return titleArray;
	}
}
