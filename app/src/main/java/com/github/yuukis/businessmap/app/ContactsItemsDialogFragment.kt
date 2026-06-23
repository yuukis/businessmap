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

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.core.os.BundleCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.Serializable

class ContactsItemsDialogFragment : DialogFragment() {

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
            contactsItems = BundleCompat.getSerializable(
                args, KEY_CONTACTS_ITEMS, Serializable::class.java
            ) as? List<ContactsItem>
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val items = contactsItems.orEmpty()

        val view = ComposeView(activity).apply {
            setContent {
                AppTheme {
                    Surface(color = Color.Transparent) {
                        ContactsItemsList(
                            items = items,
                            onItemClick = { contact ->
                                listener?.onContactsSelected(contact)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }

        return MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_select_contacts)
            .setView(view)
            .create()
    }

    companion object {
        private const val KEY_CONTACTS_ITEMS = "contacts_items"
        private const val TAG = "ContactsItemsDialogFragment"

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

@Composable
private fun ContactsItemsList(
    items: List<ContactsItem>,
    onItemClick: (ContactsItem) -> Unit
) {
    LazyColumn {
        items(items) { contact ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(contact) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = contact.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
