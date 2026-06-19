package com.github.yuukis.businessmap.util

import android.content.Context
import android.provider.ContactsContract.Groups
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup

object ContactUtils {

    @JvmStatic
    fun getContactsGroupList(context: Context): List<ContactsGroup> {
        val list = ArrayList<ContactsGroup>()
        val all = ContactsGroup(
            ContactsGroup.ID_GROUP_ALL_CONTACTS,
            context.getString(R.string.group_all_contacts), ""
        )
        list.add(all)

        val groupCursor = context.contentResolver.query(
            Groups.CONTENT_URI,
            arrayOf(Groups._ID, Groups.TITLE, Groups.ACCOUNT_NAME),
            "${Groups.DELETED}=0",
            null,
            null
        )
        try {
            while (groupCursor != null && groupCursor.moveToNext()) {
                val id = groupCursor.getLong(0)
                val title = groupCursor.getString(1)
                val accountName = groupCursor.getString(2)
                list.add(ContactsGroup(id, title, accountName))
            }
        } finally {
            groupCursor?.close()
        }
        return list
    }
}
