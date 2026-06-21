package com.github.yuukis.businessmap.util

import com.github.yuukis.businessmap.model.ContactsItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Locale

class ContactsItemComparatorTest {

    private lateinit var originalLocale: Locale

    @Before
    fun setLocale() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.JAPAN)
    }

    @After
    fun restoreLocale() {
        Locale.setDefault(originalLocale)
    }

    private fun item(cid: Long, name: String?, phonetic: String?) = ContactsItem(
        cid = cid,
        name = name,
        phonetic = phonetic,
        groupId = 0L,
        address = null,
        note = null,
        companyName = null
    )

    @Test
    fun `sorts by phonetic when present`() {
        val a = item(1L, "鈴木一郎", "スズキイチロウ")
        val b = item(2L, "山田太郎", "ヤマダタロウ")

        val result = listOf(b, a).sortedWith(ContactsItemComparator())

        assertEquals(listOf(a, b), result)
    }

    @Test
    fun `falls back to name when phonetic is missing`() {
        val a = item(1L, "あ", null)
        val b = item(2L, "ん", "")

        val result = listOf(b, a).sortedWith(ContactsItemComparator())

        assertEquals(listOf(a, b), result)
    }

    @Test
    fun `items without sort key sort after items with one`() {
        val withName = item(1L, "あ", null)
        val withoutName = item(2L, null, null)

        val result = listOf(withoutName, withName).sortedWith(ContactsItemComparator())

        assertEquals(listOf(withName, withoutName), result)
    }

    @Test
    fun `breaks ties on equal sort key by cid`() {
        val first = item(1L, "同じ名前", "オナジナマエ")
        val second = item(2L, "同じ名前", "オナジナマエ")

        val result = listOf(second, first).sortedWith(ContactsItemComparator())

        assertEquals(listOf(first, second), result)
    }

    @Test
    fun `items with no sort key at all fall back to cid order`() {
        val a = item(1L, null, null)
        val b = item(2L, null, null)

        assertTrue(ContactsItemComparator().compare(a, b) < 0)
    }
}
