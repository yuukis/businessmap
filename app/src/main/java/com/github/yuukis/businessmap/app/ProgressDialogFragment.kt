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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.fragment.app.DialogFragment
import com.github.yuukis.businessmap.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class ProgressDialogFragment : DialogFragment() {

    interface ProgressDialogFragmentListener {
        fun onProgressCancelled()
    }

    private var max = 0
    private val progressState = mutableIntStateOf(0)

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val args = requireArguments()
        val cancelable = args.getBoolean(CANCELABLE, false)
        setCancelable(cancelable)

        max = args.getInt(MAX)
        val title = args.getString(TITLE)
        val message = args.getString(MESSAGE).orEmpty()

        val view = ComposeView(requireContext()).apply {
            setContent {
                MaterialTheme {
                    Surface(color = Color.Transparent) {
                        ProgressContent(
                            message = message,
                            progress = progressState.intValue,
                            max = max
                        )
                    }
                }
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireActivity())
            .setTitle(title)
            .setView(view)
            .create()
        dialog.setCanceledOnTouchOutside(false)
        return dialog
    }

    fun updateProgress(value: Int) {
        progressState.intValue = value
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

@Composable
private fun ProgressContent(message: String, progress: Int, max: Int) {
    Column(
        modifier = Modifier.padding(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 16.dp)
    ) {
        Text(
            text = message,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )
        LinearProgressIndicator(
            progress = { if (max > 0) progress.toFloat() / max else 0f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp)
        )
        Text(
            text = stringResource(R.string.format_progress_count, progress, max),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
