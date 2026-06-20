/*
 * ProgressDialogFragment.kt
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
import android.content.DialogInterface
import android.os.Bundle
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.github.yuukis.businessmap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator

class ProgressDialogFragment : DialogFragment() {

    interface ProgressDialogFragmentListener {
        fun onProgressCancelled()
    }

    private var progressIndicator: LinearProgressIndicator? = null
    private var progressCountView: TextView? = null
    private var max = 0

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val cancelable = args.getBoolean(CANCELABLE, false)
        setCancelable(cancelable)

        max = args.getInt(MAX)
        val title = args.getString(TITLE)
        val message = args.getString(MESSAGE)
        val view = layoutInflater.inflate(R.layout.dialog_progress, null)
        view.findViewById<TextView>(R.id.textview_progress_message).text = message
        progressIndicator = view.findViewById<LinearProgressIndicator>(R.id.progress_indicator).apply {
            isIndeterminate = false
            max = this@ProgressDialogFragment.max
        }
        progressCountView = view.findViewById<TextView>(R.id.textview_progress_count).apply {
            text = getString(R.string.format_progress_count, 0, this@ProgressDialogFragment.max)
        }

        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(title)
            .setView(view)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun updateProgress(value: Int) {
        progressIndicator?.setProgressCompat(value, true)
        progressCountView?.text = getString(R.string.format_progress_count, value, max)
    }

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        getProgressDialogFragmentListener()?.onProgressCancelled()
    }

    private fun getProgressDialogFragmentListener(): ProgressDialogFragmentListener? {
        return activity as? ProgressDialogFragmentListener
    }

    companion object {
        const val TITLE = "title"
        const val MESSAGE = "message"
        const val MAX = "max"
        const val CANCELABLE = "cancelable"

        const val TAG = "ProgressDialogFragment"

        @JvmStatic
        fun newInstance(): ProgressDialogFragment {
            return ProgressDialogFragment()
        }
    }
}
