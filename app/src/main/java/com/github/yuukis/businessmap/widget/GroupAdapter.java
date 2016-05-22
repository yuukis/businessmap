/*
 * GroupAdapter.java
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
package com.github.yuukis.businessmap.widget;

import java.util.List;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.model.ContactsGroup;

import android.app.Activity;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class GroupAdapter extends BaseAdapter {

	private static final int LAYOUT_SPINNER_ITEM_RESOURCE_ID = R.layout.simple_spinner_item;
	private static final int LAYOUT_SPINNER_DROPDOWN_ITEM_RESOURCE_ID = R.layout.simple_spinner_dropdown_item_2line;

	private Activity activity;
	private List<ContactsGroup> groupList;

	public GroupAdapter(Activity activity, List<ContactsGroup> groupList) {
		this.activity = activity;
		this.groupList = groupList;
	}

	@Override
	public int getCount() {
		return groupList.size();
	}

	@Override
	public Object getItem(int position) {
		return groupList.get(position);
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = activity.getLayoutInflater().inflate(
					LAYOUT_SPINNER_ITEM_RESOURCE_ID, null);
			holder = new ViewHolder();
			holder.textView1 = (TextView) convertView
					.findViewById(android.R.id.text1);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		ContactsGroup group = (ContactsGroup) getItem(position);
		String title = group.getTitle();
		holder.textView1.setText(title);

		return convertView;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		ViewHolder holder;

		if (convertView == null) {
			convertView = activity.getLayoutInflater().inflate(
					LAYOUT_SPINNER_DROPDOWN_ITEM_RESOURCE_ID, null);
			holder = new ViewHolder();
			holder.textView1 = (TextView) convertView
					.findViewById(android.R.id.text1);
			holder.textView2 = (TextView) convertView
					.findViewById(android.R.id.text2);
			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}

		ContactsGroup group = (ContactsGroup) getItem(position);

		String title = group.getTitle();
		holder.textView1.setText(title);

		String accountName = group.getAccountName();
		if (TextUtils.isEmpty(accountName)) {
			holder.textView2.setVisibility(View.GONE);
			holder.textView2.setText("");
		} else {
			holder.textView2.setVisibility(View.VISIBLE);
			holder.textView2.setText(accountName);
		}

		return convertView;
	}

	public void setList(List<ContactsGroup> groupList) {
		this.groupList = groupList;
	}

	private static class ViewHolder {
		TextView textView1;
		TextView textView2;
	}

}
