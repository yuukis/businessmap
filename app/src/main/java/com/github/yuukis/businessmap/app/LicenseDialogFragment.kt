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
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

        applyEdgeToEdgeInsets(view, toolbar)
        return view
    }

    /**
     * Same technique as MainActivity: the toolbar's fixed height must grow
     * by the status bar inset (not just gain top padding within a fixed
     * height), otherwise its title gets squeezed and clipped.
     */
    private fun applyEdgeToEdgeInsets(root: View, toolbar: View) {
        val toolbarContentHeight = toolbar.layoutParams.height
        ViewCompat.setOnApplyWindowInsetsListener(root) { v, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            toolbar.layoutParams = toolbar.layoutParams.apply {
                height = toolbarContentHeight + bars.top
            }
            toolbar.setPadding(toolbar.paddingLeft, bars.top, toolbar.paddingRight, toolbar.paddingBottom)
            v.setPadding(bars.left, 0, bars.right, bars.bottom)
            WindowInsetsCompat.CONSUMED
        }
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
