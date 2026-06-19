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

import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.DialogFragment
import com.github.yuukis.businessmap.R

class AboutDialogFragment : DialogFragment(), View.OnClickListener {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val activity = requireActivity()
        val inflater = activity.layoutInflater
        val view = inflater.inflate(R.layout.fragment_about, null)
        view.findViewById<android.widget.TextView>(R.id.textview_about_version).text = getVersion()
        view.findViewById<View>(R.id.textview_license).setOnClickListener(this)

        return AlertDialog.Builder(activity)
            .setTitle(R.string.action_about)
            .setView(view)
            .setPositiveButton(android.R.string.ok, null)
            .create()
    }

    override fun onClick(v: View) {
        // オープンソース ライセンス表示
        LicenseDialogFragment.showDialog(requireActivity())
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
