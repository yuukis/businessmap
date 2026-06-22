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
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
import com.github.yuukis.businessmap.util.ContactUtils
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ContactsGroupDialogFragment : DialogFragment() {

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
        val activity = requireActivity()
        val groups = groupList

        val view = ComposeView(activity).apply {
            setContent {
                MaterialTheme {
                    Surface(color = Color.Transparent) {
                        ContactsGroupList(
                            groups = groups,
                            onItemClick = { group ->
                                listener?.onContactsGroupSelected(group)
                                dismiss()
                            }
                        )
                    }
                }
            }
        }

        return MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_select_group)
            .setView(view)
            .setNegativeButton(android.R.string.cancel) { _, _ ->
                listener?.onContactsGroupSelected(null)
            }
            .create()
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

@Composable
private fun ContactsGroupList(
    groups: List<ContactsGroup>,
    onItemClick: (ContactsGroup) -> Unit
) {
    LazyColumn {
        items(groups) { group ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(group) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = group.title.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}
