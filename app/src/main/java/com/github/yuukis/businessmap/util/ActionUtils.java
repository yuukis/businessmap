/*
 * ActionUtils.java
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
package com.github.yuukis.businessmap.util;

import java.util.Locale;

import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.ContactsContract.Contacts;

import com.github.yuukis.businessmap.model.ContactsItem;

public class ActionUtils {

	public static void doShowContact(Context context, ContactsItem contact) {
		Uri contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI,
				contact.getCID());
		Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
		context.startActivity(intent);
	}

	public static void doShowDirections(Context context, ContactsItem contact) {
		Uri uri = Uri.parse(String.format(Locale.getDefault(),
				"http://maps.google.com/maps?saddr=&daddr=%f,%f",
				contact.getLat(), contact.getLng()));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		context.startActivity(intent);
	}

	public static void doStartDriveNavigation(Context context,
			ContactsItem contact) {
		Uri uri = Uri.parse(String.format(Locale.getDefault(),
				"google.navigation:///?ll=%f,%f&q=%s",
				contact.getLat(), contact.getLng(), contact.getName()));
		Intent intent = new Intent(Intent.ACTION_VIEW, uri);
		intent.setClassName("com.google.android.apps.maps",
				"com.google.android.maps.driveabout.app.NavigationActivity");
		context.startActivity(intent);
	}

}
