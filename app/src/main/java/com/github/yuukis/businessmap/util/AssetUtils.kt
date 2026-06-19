/*
 * AssetUtils.kt
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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

object AssetUtils {

    @JvmStatic
    fun getText(context: Context, path: String): String? {
        val am = context.resources.assets
        val sb = StringBuilder()
        try {
            var br: BufferedReader? = null
            try {
                val stream = am.open(path)
                br = BufferedReader(InputStreamReader(stream))
                var str: String?
                while (br.readLine().also { str = it } != null) {
                    sb.append(str).append("\n")
                }
            } finally {
                br?.close()
            }
        } catch (e: IOException) {
            return null
        }
        return sb.toString()
    }

    @JvmStatic
    fun getImage(context: Context, path: String): Bitmap? {
        val am = context.resources.assets
        return try {
            val stream = am.open(path)
            BitmapFactory.decodeStream(stream)
        } catch (e: IOException) {
            null
        }
    }
}
