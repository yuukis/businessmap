/*
 * LocationActionFragment.kt
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
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Streetview
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
import com.github.yuukis.businessmap.util.ActionUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

private data class LocationActionItem(val itemId: Int, val icon: ImageVector, val titleId: Int)

private val ACTION_ITEMS = listOf(
    LocationActionItem(LocationActionFragment.ID_REGISTER_CONTACT, Icons.Default.PersonAdd, R.string.action_register_contact),
    LocationActionItem(LocationActionFragment.ID_DIRECTION, Icons.Default.Directions, R.string.action_directions),
    LocationActionItem(LocationActionFragment.ID_NAVIGATION, Icons.Default.Navigation, R.string.action_drive_navigation),
    LocationActionItem(LocationActionFragment.ID_STREETVIEW, Icons.Default.Streetview, R.string.action_street_view)
)

class LocationActionFragment : BottomSheetDialogFragment() {

    private var lat: Double = 0.0
    private var lng: Double = 0.0
    private var address: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val args = requireArguments()
        lat = args.getDouble(KEY_LAT)
        lng = args.getDouble(KEY_LNG)
        address = args.getString(KEY_ADDRESS)

        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    Surface {
                        LocationActionContent(
                            title = address?.ifEmpty { stringResource(R.string.label_selected_location) }
                                ?: stringResource(R.string.label_selected_location),
                            onActionClick = ::onActionClick
                        )
                    }
                }
            }
        }
    }

    private fun onActionClick(itemId: Int) {
        val context = requireActivity()

        when (itemId) {
            ID_REGISTER_CONTACT -> ActionUtils.doRegisterContact(context, address)
            ID_DIRECTION -> ActionUtils.doShowDirections(context, lat, lng)
            ID_NAVIGATION -> ActionUtils.doStartDriveNavigation(context, lat, lng, address.orEmpty())
            ID_STREETVIEW -> ActionUtils.doShowStreetView(context, lat, lng)
        }
        dismiss()
    }

    companion object {
        private const val TAG = "LocationActionFragment"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_ADDRESS = "address"
        const val ID_REGISTER_CONTACT = 1
        const val ID_DIRECTION = 2
        const val ID_NAVIGATION = 3
        const val ID_STREETVIEW = 4

        @JvmStatic
        fun newInstance(lat: Double, lng: Double, address: String?): LocationActionFragment {
            val fragment = LocationActionFragment()
            val args = Bundle()
            args.putDouble(KEY_LAT, lat)
            args.putDouble(KEY_LNG, lng)
            args.putString(KEY_ADDRESS, address)
            fragment.arguments = args
            return fragment
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity, lat: Double, lng: Double, address: String?) {
            val manager = activity.supportFragmentManager
            newInstance(lat, lng, address).show(manager, TAG)
        }
    }
}

@Composable
private fun LocationActionContent(
    title: String,
    onActionClick: (itemId: Int) -> Unit
) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
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
                LocationActionGridItem(data = data, onClick = { onActionClick(data.itemId) })
            }
        }
    }
}

@Composable
private fun LocationActionGridItem(
    data: LocationActionItem,
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
