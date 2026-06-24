/*
 * ContactPhotoLoader.kt
 *
 * Copyright 2026 Yuuki Shimizu.
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

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.ContactsContract.Contacts
import android.util.LruCache
import java.io.FileNotFoundException

class ContactPhotoLoader(context: Context) {

    private val contentResolver = context.applicationContext.contentResolver
    private val bitmapCache = LruCache<Long, Bitmap>(MAX_CACHE_ENTRIES)
    private val contactsWithoutPhoto = HashSet<Long>()

    fun loadThumbnail(contactId: Long): Bitmap? {
        bitmapCache[contactId]?.let { return it }
        if (contactsWithoutPhoto.contains(contactId)) {
            return null
        }

        val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
        val bitmap = try {
            Contacts.openContactPhotoInputStream(contentResolver, contactUri, false)
                ?.use(BitmapFactory::decodeStream)
        } catch (_: FileNotFoundException) {
            null
        } catch (_: SecurityException) {
            null
        }

        if (bitmap == null) {
            contactsWithoutPhoto.add(contactId)
        } else {
            bitmapCache.put(contactId, bitmap)
        }
        return bitmap
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 32
    }
}
