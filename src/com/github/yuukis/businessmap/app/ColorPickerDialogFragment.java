/*
 * ColorPickerDialogFragment.java
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
import com.github.yuukis.businessmap.util.DrawableUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;

public class ColorPickerDialogFragment extends DialogFragment {

	private static final String TAG = "ColorPickerDialogFragment";
	private static final String KEY_CONTACTS = "contacts";
	private static final int NUMBER_OF_COLUMNS = 3;

	public static ColorPickerDialogFragment newInstance(ContactsItem contact) {
		ColorPickerDialogFragment fragment = new ColorPickerDialogFragment();
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

		int layoutResID = R.layout.gridview_contents;
		TypedArray hueArray = getResources().obtainTypedArray(R.array.marker_hue);
		MenuAdapter adapter = new MenuAdapter(getActivity(), layoutResID, hueArray);
		int columns = NUMBER_OF_COLUMNS;
		GridView gridView = new GridView(getActivity());
		gridView.setNumColumns(columns);
		gridView.setAdapter(adapter);
		String title = mContact.getName();

		return new AlertDialog.Builder(getActivity())
				.setTitle(title)
				.setView(gridView)
				.setNegativeButton(android.R.string.cancel, null)
				.create();
	}

	private static class ViewHolder {
		ImageView imageView;
		TextView textView;
	}

	private class MenuAdapter extends BaseAdapter {

		private Context context;
		private LayoutInflater inflater;
		private int layoutId;
		private TypedArray hueArray;

		public MenuAdapter(Context context, int resource, TypedArray hueArray) {
			this.context = context;
			this.inflater = (LayoutInflater) context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			this.layoutId = resource;
			this.hueArray = hueArray;
		}

		@Override
		public int getCount() {
			return hueArray.length();
		}

		@Override
		public Object getItem(int position) {
			return hueArray.getFloat(position, 0.0f);
		}

		@Override
		public long getItemId(int position) {
			return position;
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
			float hue = (Float) getItem(position);
			Drawable drawable = DrawableUtils.getCircleDrawable(context, hue);

			//holder.textView.setText(hue.titleId);
			holder.imageView.setImageDrawable(drawable);

			return convertView;
		}
	}

}
