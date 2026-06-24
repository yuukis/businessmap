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
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import android.provider.ContactsContract.Contacts
import android.util.LruCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

class ContactPhotoLoader internal constructor(
    private val photoReader: (Long) -> Bitmap?
) {

    private val bitmapCache = LruCache<Long, Bitmap>(MAX_CACHE_ENTRIES)
    private val contactsWithoutPhoto = HashSet<Long>()

    constructor(context: Context) : this(createPhotoReader(context))

    @Synchronized
    fun getCachedThumbnail(contactId: Long): Bitmap? = bitmapCache[contactId]

    @Synchronized
    fun isLoadCompleted(contactId: Long): Boolean =
        bitmapCache[contactId] != null || contactsWithoutPhoto.contains(contactId)

    suspend fun loadThumbnailAsync(contactId: Long): Bitmap? = withContext(Dispatchers.IO) {
        loadThumbnail(contactId)
    }

    @Synchronized
    fun loadThumbnail(contactId: Long): Bitmap? {
        bitmapCache[contactId]?.let { return it }
        if (contactsWithoutPhoto.contains(contactId)) {
            return null
        }

        val bitmap = photoReader(contactId)?.let(::cropToCircle)

        if (bitmap == null) {
            contactsWithoutPhoto.add(contactId)
        } else {
            bitmapCache.put(contactId, bitmap)
        }
        return bitmap
    }

    companion object {
        private const val MAX_CACHE_ENTRIES = 32

        internal fun cropToCircle(source: Bitmap): Bitmap {
            val size = minOf(source.width, source.height)
            val result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            val matrix = Matrix().apply {
                setTranslate(
                    (size - source.width) / 2f,
                    (size - source.height) / 2f
                )
            }
            shader.setLocalMatrix(matrix)

            Canvas(result).drawCircle(
                size / 2f,
                size / 2f,
                size / 2f,
                Paint(Paint.ANTI_ALIAS_FLAG).apply { this.shader = shader }
            )
            return result
        }

        private fun createPhotoReader(context: Context): (Long) -> Bitmap? {
            val contentResolver = context.applicationContext.contentResolver
            return { contactId ->
                val contactUri = ContentUris.withAppendedId(Contacts.CONTENT_URI, contactId)
                try {
                    Contacts.openContactPhotoInputStream(contentResolver, contactUri, false)
                        ?.use(BitmapFactory::decodeStream)
                } catch (_: FileNotFoundException) {
                    null
                } catch (_: SecurityException) {
                    null
                }
            }
        }
    }
}
