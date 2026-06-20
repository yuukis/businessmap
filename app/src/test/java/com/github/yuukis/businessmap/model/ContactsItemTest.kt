package com.github.yuukis.businessmap.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ContactsItemTest {

    private fun newItem(groupId: Long = 1L) = ContactsItem(
        cid = 100L,
        name = "山田太郎",
        phonetic = "ヤマダタロウ",
        groupId = groupId,
        address = "東京都",
        note = "note",
        companyName = "会社"
    ).apply {
        setLat(35.0)
        setLng(139.0)
    }

    @Test
    fun `equals ignores groupId`() {
        val a = newItem(groupId = 1L)
        val b = newItem(groupId = 2L)

        assertEquals(a, b)
    }

    @Test
    fun `hashCode ignores groupId`() {
        val a = newItem(groupId = 1L)
        val b = newItem(groupId = 2L)

        assertEquals(a.hashCode(), b.hashCode())
    }

    @Test
    fun `equals detects differing cid`() {
        val a = newItem()
        val b = ContactsItem(
            cid = 999L,
            name = a.name,
            phonetic = a.phonetic,
            groupId = a.groupId,
            address = a.address,
            note = a.note,
            companyName = a.companyName
        ).apply {
            setLat(35.0)
            setLng(139.0)
        }

        assertNotEquals(a, b)
    }

    @Test
    fun `setLat and setLng treat NaN as null`() {
        val item = newItem()
        item.setLat(Double.NaN)
        item.setLng(Double.NaN)

        assertEquals(null, item.lat)
        assertEquals(null, item.lng)
    }
}
