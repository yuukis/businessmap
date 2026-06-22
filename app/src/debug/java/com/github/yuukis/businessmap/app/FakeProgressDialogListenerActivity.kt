package com.github.yuukis.businessmap.app

import androidx.fragment.app.FragmentActivity

/**
 * Minimal test-only host for [ProgressDialogFragment] that records whether
 * [ProgressDialogFragment.ProgressDialogFragmentListener.onProgressCancelled]
 * fired, so tests can verify cancellation without depending on
 * [MainActivity]'s full geocoding flow.
 */
class FakeProgressDialogListenerActivity :
    FragmentActivity(),
    ProgressDialogFragment.ProgressDialogFragmentListener {

    var cancelledCallbackCount: Int = 0
        private set

    override fun onProgressCancelled() {
        cancelledCallbackCount++
    }
}
