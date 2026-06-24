/*
 * IconUtils.kt
 *
 * Copyright 2025 Yuuki Shimizu.
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
import android.content.pm.ShortcutManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import java.util.Locale

object IconUtils {

    // Fallback used when ShortcutManager is unavailable (e.g. feature not supported).
    private const val FALLBACK_ICON_SIZE_DP = 108
    private val BACKGROUND_COLOR = Color.parseColor("#3F51B5")

    @JvmStatic
    fun createInitialIconBitmap(context: Context, label: String?): Bitmap {
        val initial = label?.trim()?.firstOrNull()?.toString()?.uppercase(Locale.ROOT) ?: "?"
        val iconSize = resolveIconSizePx(context)

        val bitmap = Bitmap.createBitmap(iconSize, iconSize, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val backgroundPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = BACKGROUND_COLOR
        }
        val radius = iconSize / 2f
        canvas.drawCircle(radius, radius, radius, backgroundPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = iconSize * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
        }
        val textY = radius - (textPaint.descent() + textPaint.ascent()) / 2f
        canvas.drawText(initial, radius, textY, textPaint)

        return bitmap
    }

    private fun resolveIconSizePx(context: Context): Int {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java)
        val maxWidth = shortcutManager?.iconMaxWidth ?: 0
        val maxHeight = shortcutManager?.iconMaxHeight ?: 0
        if (maxWidth > 0 && maxHeight > 0) {
            return minOf(maxWidth, maxHeight)
        }
        val density = context.resources.displayMetrics.density
        return (FALLBACK_ICON_SIZE_DP * density).toInt()
    }
}
