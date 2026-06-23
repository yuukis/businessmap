/*
 * ActionUtils.kt
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
package com.github.yuukis.businessmap.util

import android.content.ActivityNotFoundException
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract.Contacts
import com.github.yuukis.businessmap.model.ContactsItem
import java.util.Locale

object ActionUtils {

    @JvmStatic
    fun doShowContact(context: Context, contact: ContactsItem) {
        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contact.cid)
        val intent = Intent(Intent.ACTION_VIEW, contactUri)
        context.startActivity(intent)
    }

    @JvmStatic
    fun doShowDirections(context: Context, contact: ContactsItem) {
        val uri = Uri.parse(
            String.format(
                Locale.US,
                "http://maps.google.com/maps?saddr=&daddr=%f,%f",
                contact.lat, contact.lng
            )
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        context.startActivity(intent)
    }

    @JvmStatic
    fun doStartDriveNavigation(context: Context, contact: ContactsItem) {
        val uri = Uri.parse(
            String.format(
                Locale.US,
                "google.navigation:///?ll=%f,%f&q=%s",
                contact.lat, contact.lng, contact.name
            )
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setClassName(
            "com.google.android.apps.maps",
            "com.google.android.maps.driveabout.app.NavigationActivity"
        )
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Google Maps is not installed; nothing we can do.
        }
    }

    @JvmStatic
    fun doShowStreetView(context: Context, contact: ContactsItem) {
        val uri = Uri.parse(
            String.format(
                Locale.US,
                "google.streetview:cbll=%f,%f",
                contact.lat, contact.lng
            )
        )
        val intent = Intent(Intent.ACTION_VIEW, uri)
        intent.setPackage("com.google.android.apps.maps")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // Google Maps is not installed; nothing we can do.
        }
    }
}
