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
import android.app.ProgressDialog
import android.content.DialogInterface
import android.os.Bundle
import androidx.fragment.app.DialogFragment

class ProgressDialogFragment : DialogFragment() {

    interface ProgressDialogFragmentListener {
        fun onProgressCancelled()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val cancelable = args.getBoolean(CANCELABLE, false)
        setCancelable(cancelable)

        val title = args.getString(TITLE)
        val message = args.getString(MESSAGE)
        val dialog = ProgressDialog(activity)
        dialog.setTitle(title)
        dialog.setMessage(message)
        dialog.isIndeterminate = false
        dialog.setCanceledOnTouchOutside(false)

        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
        dialog.max = args.getInt(MAX)

        return dialog
    }

    fun updateProgress(value: Int) {
        val dialog = dialog as? ProgressDialog
        dialog?.progress = value
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
