/*
 * ContactsItemsDialogFragment.kt
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
package com.github.yuukis.businessmap.app

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import java.io.Serializable

class ContactsItemsDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    interface OnSelectListener {
        fun onContactsSelected(contacts: ContactsItem?)
    }

    private var listener: OnSelectListener? = null
    private var contactsItems: List<ContactsItem>? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectListener) {
            listener = context
        }
        val args = arguments
        if (args != null && args.containsKey(KEY_CONTACTS_ITEMS)) {
            @Suppress("UNCHECKED_CAST")
            contactsItems = args.get(KEY_CONTACTS_ITEMS) as? List<ContactsItem>
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = getContactsItemsTitleArray()
        return AlertDialog.Builder(activity)
            .setTitle(R.string.action_select_contacts)
            .setItems(items, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val currentListener = listener ?: return
        val contacts = if (which != DialogInterface.BUTTON_NEGATIVE) contactsItems?.get(which) else null
        currentListener.onContactsSelected(contacts)
    }

    private fun getContactsItemsTitleArray(): Array<CharSequence?> {
        val list = contactsItems ?: return arrayOf()
        return Array(list.size) { i -> list[i].name }
    }

    companion object {
        private const val KEY_CONTACTS_ITEMS = "contacts_items"
        private const val TAG = "ContactsGroupDialogFragment"

        @JvmStatic
        fun newInstance(contactsList: List<ContactsItem>): ContactsItemsDialogFragment {
            val dialogFragment = ContactsItemsDialogFragment()
            val args = Bundle()
            args.putSerializable(KEY_CONTACTS_ITEMS, contactsList as Serializable)
            dialogFragment.arguments = args
            return dialogFragment
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity, contactsList: List<ContactsItem>) {
            val manager = activity.supportFragmentManager
            newInstance(contactsList).show(manager, TAG)
        }
    }
}
