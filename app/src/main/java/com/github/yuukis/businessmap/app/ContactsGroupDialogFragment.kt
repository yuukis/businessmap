/*
 * ContactsGroupDialogFragment.kt
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
package com.github.yuukis.businessmap.app

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.util.ContactUtils

class ContactsGroupDialogFragment : DialogFragment(), DialogInterface.OnClickListener {

    interface OnSelectListener {
        fun onContactsGroupSelected(group: ContactsGroup?)
    }

    private var listener: OnSelectListener? = null
    private lateinit var groupList: List<ContactsGroup>

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnSelectListener) {
            listener = context
        }
        groupList = ContactUtils.getContactsGroupList(context)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val items = getContactsGroupTitleArray()
        return AlertDialog.Builder(activity)
            .setTitle(R.string.action_select_group)
            .setItems(items, this)
            .setNegativeButton(android.R.string.cancel, this)
            .create()
    }

    override fun onClick(dialog: DialogInterface, which: Int) {
        val currentListener = listener ?: return
        val group = if (which != DialogInterface.BUTTON_NEGATIVE) groupList[which] else null
        currentListener.onContactsGroupSelected(group)
    }

    private fun getContactsGroupTitleArray(): Array<CharSequence?> {
        return Array(groupList.size) { i -> groupList[i].title }
    }

    companion object {
        private const val TAG = "ContactsGroupDialogFragment"

        @JvmStatic
        fun newInstance(): ContactsGroupDialogFragment {
            return ContactsGroupDialogFragment()
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity) {
            val manager = activity.supportFragmentManager
            newInstance().show(manager, TAG)
        }
    }
}
