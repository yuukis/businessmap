/*
 * AssetUtils.java
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
