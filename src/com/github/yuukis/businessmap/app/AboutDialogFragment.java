/*
 * AboutDialogFragment.java
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

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

public class AboutDialogFragment extends DialogFragment implements
		View.OnClickListener {

	private static final String TAG = "AboutDialogFragment";

	public static AboutDialogFragment newInstance() {
		return new AboutDialogFragment();
	}

	public static void showDialog(Activity activity) {
		FragmentManager manager = activity.getFragmentManager();
		newInstance().show(manager, TAG);
	}

	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {
		LayoutInflater inflater = getActivity().getLayoutInflater();
		View view = inflater.inflate(R.layout.fragment_about, null);
		((TextView) view.findViewById(R.id.textview_about_version))
				.setText(getVersion());
		view.findViewById(R.id.textview_license).setOnClickListener(this);

		return new AlertDialog.Builder(getActivity())
				.setTitle(R.string.action_about).setView(view)
				.setPositiveButton(android.R.string.ok, null).create();
	}

	@Override
	public void onClick(View v) {
		// オープンソース ライセンス表示
		LicenseDialogFragment.showDialog(getActivity());
	}

	private String getVersion() {
		Context context = getActivity();
		String version = "---";
		try {
			PackageInfo packageInfo = context.getPackageManager()
					.getPackageInfo(context.getPackageName(),
							PackageManager.GET_META_DATA);
			version = packageInfo.versionName;
		} catch (NameNotFoundException e) {
		}
		return version;
	}

}
