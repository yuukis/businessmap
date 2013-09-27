package com.github.yuukis.businessmap.controller;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

import com.github.yuukis.businessmap.R;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;

public class UpdateAppTask extends AsyncTask<String, Void, File> {

	private static final String FILENAME_APP_APK = "BusinessMap.apk";

	private Context mContext;
	private ProgressDialog mProgressDialog;

	public static void execute(Context context) {
		new UpdateAppTask(context).execute();
	}

	public UpdateAppTask(Context context) {
		mContext = context;

		mProgressDialog = new ProgressDialog(context);
		mProgressDialog.setMessage(context
				.getString(R.string.beta__message_downloading));
	}

	public void onPreExecute() {
		if (mProgressDialog != null)
			mProgressDialog.show();
	}

	@Override
	public void onProgressUpdate(Void... values) {
	}

	@Override
	public File doInBackground(String... params) {
		String url = mContext.getString(R.string.beta__apk_url);

		try {
			HttpURLConnection c = (HttpURLConnection) new URL(url)
					.openConnection();
			c.setRequestMethod("GET");
			c.setDoOutput(true);
			c.connect();

			String path = Environment.getExternalStorageDirectory()
					+ "/download/";
			File file = new File(path);
			file.mkdirs();
			File outputFile = new File(file, FILENAME_APP_APK);
			FileOutputStream fos = new FileOutputStream(outputFile);

			InputStream is = c.getInputStream();

			byte[] buffer = new byte[1024];
			int len1 = 0;
			while ((len1 = is.read(buffer)) != -1) {
				fos.write(buffer, 0, len1);
			}
			fos.close();
			is.close();

			return outputFile;

		} catch (IOException e) {

			return null;
		}
	}

	public void onPostExecute(File apkFile) {
		if (mProgressDialog != null) {
			mProgressDialog.dismiss();
		}
		if (apkFile != null) {
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setDataAndType(Uri.fromFile(apkFile),
					"application/vnd.android.package-archive");
			mContext.startActivity(intent);
		}
	}

}
