package com.github.yuukis.businessmap.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class AssetUtils {
	public static String getText(Context context, String path) {
		AssetManager as = context.getResources().getAssets();

		InputStream is = null;
		BufferedReader br = null;

		StringBuilder sb = new StringBuilder();
		try {
			try {
				is = as.open(path);
				br = new BufferedReader(new InputStreamReader(is));

				String str;
				while ((str = br.readLine()) != null) {
					sb.append(str + "\n");
				}
			} finally {
				if (br != null)
					br.close();
			}
		} catch (IOException e) {
			return null;
		}

		return sb.toString();
	}

	public static Bitmap getImage(Context context, String path) {
		AssetManager as = context.getResources().getAssets();

		InputStream is = null;
		Bitmap bitmap = null;
		try {
			is = as.open(path);
			bitmap = BitmapFactory.decodeStream(is);
		} catch (IOException e) {
			return null;
		}

		return bitmap;
	}
}
