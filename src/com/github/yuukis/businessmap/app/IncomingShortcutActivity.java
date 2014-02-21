/*
 * IncomingShortcutActivity.java
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

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;

import android.os.Bundle;
import android.os.Parcelable;
import android.app.Activity;
import android.content.Intent;

public class IncomingShortcutActivity extends Activity implements
		ContactsGroupDialogFragment.OnSelectListener {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ContactsGroupDialogFragment.showDialog(this);
	}

	@Override
	public void onContactsGroupSelected(ContactsGroup group) {
		createShortcut(group);
		finish();
	}

	private void createShortcut(ContactsGroup group) {
		if (group == null) {
			setResult(RESULT_CANCELED, null);
			return;
		}
		long groupId = group.getId();
		String shortcutTitle = group.getTitle();
		if (shortcutTitle.isEmpty()) {
			shortcutTitle = getString(R.string.app_name);
		}

		Intent shortcutIntent = new Intent(getApplicationContext(),
				MainActivity.class);
		shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
		shortcutIntent.putExtra(MainActivity.KEY_CONTACTS_GROUP_ID, groupId);

		Intent intent = new Intent();
		intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
		Parcelable iconResource = Intent.ShortcutIconResource.fromContext(
				getApplicationContext(), R.drawable.ic_launcher);
		// ホーム画面に設置した場合に表示されるアイコンの設定
		intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, iconResource);
		// ホーム画面に設置した場合に表示されるラベル名の設定
		intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, shortcutTitle);

		setResult(RESULT_OK, intent);
	}
}
