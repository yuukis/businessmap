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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.webkit.WebView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.util.AssetUtils
import com.google.android.material.appbar.MaterialToolbar
import java.io.UnsupportedEncodingException
import java.net.URLEncoder

class LicenseDialogFragment : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.AppTheme)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        var html = AssetUtils.getText(requireActivity(), FILEPATH) ?: ""
        try {
            html = URLEncoder.encode(html, "utf-8").replace("\\+".toRegex(), "%20")
        } catch (e: UnsupportedEncodingException) {
        }

        val view = inflater.inflate(R.layout.dialog_license, container, false)
        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar_license)
        toolbar.setNavigationIcon(android.R.drawable.ic_menu_close_clear_cancel)
        toolbar.setNavigationOnClickListener { dismiss() }

        val webView = view.findViewById<WebView>(R.id.webview_license)
        webView.loadData(html, "text/html", "utf-8")
        return view
    }

    companion object {
        private const val TAG = "LicenseDialogFragment"
        private const val FILEPATH = "license.html"

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
