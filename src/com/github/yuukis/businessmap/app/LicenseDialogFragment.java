package com.github.yuukis.businessmap.app;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.github.yuukis.businessmap.R;
import com.github.yuukis.businessmap.utils.AssetUtils;

import android.app.Activity;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.os.Bundle;
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

	public static void showDialog(Activity activity) {
		FragmentManager manager = activity.getFragmentManager();
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
