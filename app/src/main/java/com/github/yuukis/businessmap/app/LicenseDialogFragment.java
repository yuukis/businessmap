/*
 * LicenseDialogFragment.java
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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.util.AssetUtils;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

public class LicenseDialogFragment extends DialogFragment {

	private static final String TAG = "LicenseDialogFragment";
	private static final String FILEPATH = "license.html";

	public static LicenseDialogFragment newInstance() {
		return new LicenseDialogFragment();
	}

	public static void showDialog(FragmentActivity activity) {
		FragmentManager manager = activity.getSupportFragmentManager();
		newInstance().show(manager, TAG);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setStyle(STYLE_NORMAL | STYLE_NO_TITLE, R.style.AppTheme);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container,
			Bundle savedInstanceState) {

		String html = AssetUtils.getText(getActivity(), FILEPATH);
		try {
			html = URLEncoder.encode(html, "utf-8").replaceAll("\\+", "%20");
		} catch (UnsupportedEncodingException e) {
		}
		WebView webView = new WebView(getActivity());
		webView.loadData(html, "text/html", "utf-8");
		return webView;
	}

}
