/*
 * LicenseDialogFragment.kt
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
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.OssLicense
import com.github.yuukis.businessmap.util.OssLicensesUtils

class LicenseDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.let { window ->
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val licenses = OssLicensesUtils.loadLicenses(requireActivity())
        return ComposeView(requireContext()).apply {
            setContent {
                AppTheme {
                    LicenseScreen(licenses = licenses, onClose = { dismiss() })
                }
            }
        }
    }

    companion object {
        private const val TAG = "LicenseDialogFragment"

        @JvmStatic
        fun newInstance(): LicenseDialogFragment {
            return LicenseDialogFragment()
        }

        @JvmStatic
        fun showDialog(activity: FragmentActivity) {
            val manager = activity.supportFragmentManager
            newInstance().show(manager, TAG)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LicenseScreen(licenses: List<OssLicense>, onClose: () -> Unit) {
    var selectedIndex by rememberSaveable { mutableIntStateOf(-1) }
    val selected = licenses.getOrNull(selectedIndex)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(selected?.name ?: stringResource(R.string.title_licenses)) },
                navigationIcon = {
                    IconButton(
                        modifier = Modifier.testTag("license_nav_icon"),
                        onClick = { if (selected != null) selectedIndex = -1 else onClose() }
                    ) {
                        if (selected != null) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.desc_back))
                        } else {
                            Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.desc_close))
                        }
                    }
                }
            )
        }
    ) { padding ->
        if (selected != null) {
            LicenseDetail(license = selected, modifier = Modifier.padding(padding))
        } else {
            LicenseList(
                licenses = licenses,
                modifier = Modifier.padding(padding),
                onItemClick = { index -> selectedIndex = index }
            )
        }
    }
}

@Composable
private fun LicenseList(
    licenses: List<OssLicense>,
    modifier: Modifier = Modifier,
    onItemClick: (Int) -> Unit
) {
    if (licenses.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = stringResource(R.string.message_no_licenses))
        }
        return
    }

    LazyColumn(modifier = modifier.fillMaxSize().testTag("license_list")) {
        items(licenses.size) { index ->
            val license = licenses[index]
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("license_item_$index")
                    .clickable { onItemClick(index) }
                    .heightIn(min = 48.dp)
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Column {
                    Text(text = license.name, style = MaterialTheme.typography.titleMedium)
                    val subtitle = listOf(license.version, license.licenseName)
                        .filter { it.isNotBlank() }
                        .joinToString(" · ")
                    if (subtitle.isNotBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LicenseDetail(license: OssLicense, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("license_detail")
    ) {
        if (license.version.isNotBlank()) {
            Text(
                text = license.version,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (license.licenseName.isNotBlank()) {
            Text(
                text = license.licenseName,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        license.licenseUrl?.let { url ->
            TextButton(
                onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, url.toUri())) },
                modifier = Modifier.padding(top = 16.dp)
            ) {
                Text(stringResource(R.string.action_view_license_text))
            }
        }
    }
}
