package com.github.yuukis.businessmap.util

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactPhotoLoaderTest {

    /**
     * 一度読み込んだ連絡先画像をキャッシュし、同じ連絡先で画像を再取得しないことを確認する。
     */
    @Test
    fun cachesLoadedThumbnail() {
        val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        var readCount = 0
        val loader = ContactPhotoLoader {
            readCount++
            expected
        }

        assertSame(expected, loader.loadThumbnail(1L))
        assertSame(expected, loader.loadThumbnail(1L))
        assertEquals(1, readCount)
    }

    /**
     * 画像未登録の連絡先も結果を記憶し、InfoWindowの再描画で問い合わせを繰り返さないことを確認する。
     */
    @Test
    fun cachesMissingThumbnail() {
        var readCount = 0
        val loader = ContactPhotoLoader {
            readCount++
            null
        }

        assertNull(loader.loadThumbnail(1L))
        assertNull(loader.loadThumbnail(1L))
        assertEquals(1, readCount)
    }
}
