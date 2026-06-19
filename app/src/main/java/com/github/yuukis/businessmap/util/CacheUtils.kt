/*
 * CacheUtils.kt
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
package com.github.yuukis.businessmap.util

import android.content.Context
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

object CacheUtils {

    @JvmStatic
    @Throws(FileNotFoundException::class, IOException::class)
    fun read(context: Context, fileName: String): ByteArray {
        val file = getFile(context, fileName)
        val bos = ByteArrayOutputStream()

        val fis = FileInputStream(file)
        val data = ByteArray(fis.available())
        fis.read(data)
        bos.write(data)
        fis.close()

        return bos.toByteArray()
    }

    @JvmStatic
    @Throws(FileNotFoundException::class, IOException::class)
    fun write(context: Context, data: ByteArray?, fileName: String) {
        val file = getFile(context, fileName)

        if (data != null) {
            val fos = FileOutputStream(file)
            fos.write(data)
            fos.close()
        }
    }

    private fun getFile(context: Context, fileName: String): File {
        val dir = context.cacheDir
        dir.mkdirs()
        return File(dir, fileName)
    }
}
