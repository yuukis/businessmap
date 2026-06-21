/*
 * AboutDialogFragment.kt
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
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.DialogFragment
import com.github.yuukis.businessmap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class AboutDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val version = getVersion()

        val view = ComposeView(activity).apply {
            setContent {
                MaterialTheme {
                    Surface(color = Color.Transparent) {
                        AboutContent(
                            version = version,
                            onLicensesClick = { LicenseDialogFragment.showDialog(activity) }
                        )
                    }
                }
            }
        }

        return MaterialAlertDialogBuilder(activity)
            .setTitle(R.string.action_about)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    private fun getVersion(): String {
        val context = requireActivity()
        var version = "---"
        try {
            val packageInfo = context.packageManager
                .getPackageInfo(context.packageName, PackageManager.GET_META_DATA)
            version = packageInfo.versionName ?: version
        } catch (e: NameNotFoundException) {
        }
        return version
    }

    companion object {
        private const val TAG = "AboutDialogFragment"

        @JvmStatic
        fun newInstance(): AboutDialogFragment {
            return AboutDialogFragment()
        }

        @JvmStatic
        fun showDialog(activity: AppCompatActivity) {
            val manager = activity.supportFragmentManager
            newInstance().show(manager, TAG)
        }
    }
}

@Composable
private fun AboutContent(
    version: String,
    onLicensesClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Image(
            painter = painterResource(R.drawable.ic_launcher),
            contentDescription = stringResource(R.string.desc_appicon),
            modifier = Modifier.size(72.dp)
        )
        AboutLabelValue(stringResource(R.string.label_app_name), stringResource(R.string.app_name))
        AboutLabelValue(stringResource(R.string.label_provider), stringResource(R.string.provider))
        AboutLabelValue(stringResource(R.string.label_version), version)
        Text(
            text = stringResource(R.string.label_licenses),
            color = MaterialTheme.colorScheme.primary,
            textDecoration = TextDecoration.Underline,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = 20.dp)
                .clickable(onClick = onLicensesClick)
        )
    }
}

@Composable
private fun AboutLabelValue(label: String, value: String) {
    Text(
        text = label,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 14.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 20.dp)
    )
    Text(
        text = value,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 18.sp,
        textAlign = TextAlign.Center
    )
}
