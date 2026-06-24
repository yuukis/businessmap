package com.github.yuukis.businessmap.util

import android.graphics.Bitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
    fun cachesLoadedThumbnail() = runBlocking {
        val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        var readCount = 0
        val loader = ContactPhotoLoader {
            readCount++
            expected
        }

        val first = loader.loadThumbnailAsync(1L)
        val second = loader.loadThumbnailAsync(1L)

        assertSame(first, second)
        assertEquals(1, readCount)
    }

    /**
     * 画像未登録の連絡先も結果を記憶し、InfoWindowの再描画で問い合わせを繰り返さないことを確認する。
     */
    @Test
    fun cachesMissingThumbnail() = runBlocking {
        var readCount = 0
        val loader = ContactPhotoLoader {
            readCount++
            null
        }

        assertNull(loader.loadThumbnailAsync(1L))
        assertNull(loader.loadThumbnailAsync(1L))
        assertEquals(1, readCount)
    }

    /**
     * 連絡先画像を円形に切り抜き、四隅が透明で中央に画像が残ることを確認する。
     */
    @Test
    fun cropsThumbnailToCircle() {
        val source = Bitmap.createBitmap(10, 10, Bitmap.Config.ARGB_8888).apply {
            eraseColor(0xffff0000.toInt())
        }

        val result = ContactPhotoLoader.cropToCircle(source)

        assertEquals(0, result.getPixel(0, 0) ushr 24)
        assertEquals(0xff, result.getPixel(5, 5) ushr 24)
    }

    /**
     * 非同期読み込みの完了後に、InfoWindowの再描画で利用できる画像がキャッシュされることを確認する。
     */
    @Test
    fun cachesThumbnailLoadedAsynchronously() = runBlocking {
        val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        val loader = ContactPhotoLoader { expected }

        assertNull(loader.getCachedThumbnail(1L))
        loader.loadThumbnailAsync(1L)

        assertSame(loader.getCachedThumbnail(1L), loader.loadThumbnailAsync(1L))
        assertEquals(true, loader.isLoadCompleted(1L))
    }

    /**
     * 同じ連絡先の画像を同時に要求しても、Contacts Providerへの問い合わせが1回だけになることを確認する。
     */
    @Test
    fun sharesConcurrentThumbnailLoad() = runBlocking {
        val expected = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        var readCount = 0
        val loader = ContactPhotoLoader {
            synchronized(this) {
                readCount++
            }
            Thread.sleep(100)
            expected
        }

        val results = List(2) {
            async { loader.loadThumbnailAsync(1L) }
        }.awaitAll()

        assertSame(results[0], results[1])
        assertEquals(1, readCount)
    }
}
