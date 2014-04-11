/*
 * ProgressDialogFragment.java
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
package com.github.yuukis.businessmap.app;

import com.actionbarsherlock.app.SherlockDialogFragment;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Bundle;

public class ProgressDialogFragment extends SherlockDialogFragment {

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";
    public static final String MAX = "max";
    public static final String CANCELABLE = "cancelable";

    public static final String TAG = "ProgressDialogFragment";

    public static ProgressDialogFragment newInstance() {
        return new ProgressDialogFragment();
    }

    public interface ProgressDialogFragmentListener {
        void onProgressCancelled();
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        boolean cancelable = getArguments().getBoolean(CANCELABLE, false);
        setCancelable(cancelable);

        String title = getArguments().getString(TITLE);
        String message = getArguments().getString(MESSAGE);
        ProgressDialog dialog = new ProgressDialog(getActivity());
        dialog.setTitle(title);
        dialog.setMessage(message);
        dialog.setIndeterminate(false);
		dialog.setCanceledOnTouchOutside(false);

        dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        dialog.setMax(getArguments().getInt(MAX));

        return dialog;
    }

    public void updateProgress(int value) {
        ProgressDialog dialog = (ProgressDialog) getDialog();
        if (dialog != null) {
            dialog.setProgress(value);
        }
    }

    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        if (getProgressDialogFragmentListener() != null) {
            getProgressDialogFragmentListener().onProgressCancelled();
        }
    }

    private ProgressDialogFragmentListener getProgressDialogFragmentListener() {
        if (getActivity() == null) {
            return null;
        }

        if (getActivity() instanceof ProgressDialogFragmentListener) {
            return (ProgressDialogFragmentListener) getActivity();
        }
        return null;
    }
}
