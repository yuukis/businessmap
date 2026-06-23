/*
 * ContactsActionFragment.kt
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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.integerResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import com.github.yuukis.businessmap.util.ActionUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private data class ContactsActionItem(val itemId: Int, val icon: ImageVector, val titleId: Int)

private val ACTION_ITEMS = listOf(
    ContactsActionItem(ContactsActionFragment.ID_SHOW_CONTACTS, Icons.Default.Person, R.string.action_contacts_detail),
    ContactsActionItem(ContactsActionFragment.ID_DIRECTION, Icons.Default.Directions, R.string.action_directions),
    ContactsActionItem(ContactsActionFragment.ID_NAVIGATION, Icons.Default.Navigation, R.string.action_drive_navigation)
)

class ContactsActionFragment : BottomSheetDialogFragment() {

    private var contact: ContactsItem? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        contact = requireArguments().getSerializable(KEY_CONTACTS) as? ContactsItem

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    Surface {
                        ContactsActionContent(
                            contactName = contact?.name.orEmpty(),
                            onActionClick = ::onActionClick
                        )
                    }
                }
            }
        }
    }

    private fun onActionClick(itemId: Int) {
        val context = requireActivity()
        val item = contact ?: return

        when (itemId) {
            ID_SHOW_CONTACTS -> ActionUtils.doShowContact(context, item)
            ID_DIRECTION -> ActionUtils.doShowDirections(context, item)
            ID_NAVIGATION -> ActionUtils.doStartDriveNavigation(context, item)
        }
    }

    companion object {
        private const val TAG = "ContactsActionFragment"
        private const val KEY_CONTACTS = "contacts"
        const val ID_SHOW_CONTACTS = 1
        const val ID_DIRECTION = 2
        const val ID_NAVIGATION = 3

        @JvmStatic
        fun newInstance(contact: ContactsItem): ContactsActionFragment {
            val fragment = ContactsActionFragment()
            val args = Bundle()
            args.putSerializable(KEY_CONTACTS, contact)
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity, contact: ContactsItem) {
            val manager = activity.supportFragmentManager
            newInstance(contact).show(manager, TAG)
        }
    }
}

@Composable
private fun ContactsActionContent(
    contactName: String,
    onActionClick: (itemId: Int) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = contactName,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 8.dp)
        )

        val columns = integerResource(R.integer.gridview_columns)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(ACTION_ITEMS) { data ->
                ContactsActionGridItem(data = data, onClick = { onActionClick(data.itemId) })
            }
        }
    }
}

@Composable
private fun ContactsActionGridItem(
    data: ContactsActionItem,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp)
    ) {
        Icon(
            imageVector = data.icon,
            contentDescription = stringResource(data.titleId),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(64.dp)
        )
        Text(
            text = stringResource(data.titleId),
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
