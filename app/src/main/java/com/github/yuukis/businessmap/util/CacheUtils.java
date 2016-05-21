/*
 * CacheUtils.java
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
package com.github.yuukis.businessmap.util;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.content.Context;

public class CacheUtils {

	public static byte[] read(Context context, String fileName)
			throws FileNotFoundException, IOException {

		File file = getFile(context, fileName);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();

		if (file != null) {
			FileInputStream fis = new FileInputStream(file);
			byte[] data = new byte[fis.available()];
			fis.read(data);
			bos.write(data);
			fis.close();
		}

		return bos.toByteArray();
	}

	public static void write(Context context, byte[] data, String fileName)
			throws FileNotFoundException, IOException {

		File file = getFile(context, fileName);

		if (file != null && data != null) {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(data);
			fos.close();
		}
	}

	private static File getFile(Context context, String fileName) {

		File dir = context.getCacheDir();
		dir.mkdirs();
		File file = new File(dir, fileName);

		return file;
	}

}
