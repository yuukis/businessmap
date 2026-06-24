package com.github.yuukis.businessmap.app

import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.model.ContactsGroup

/**
 * Minimal test-only host for [ContactsGroupDialogFragment].
 *
 * Keeping the fragment test independent from [IncomingShortcutActivity] avoids
 * finishing the task under test, which can race Android Test Orchestrator's
 * package cleanup between tests.
 */
class FakeContactsGroupSelectListenerActivity :
    FragmentActivity(),
    ContactsGroupDialogFragment.OnSelectListener {

    var selectedGroup: ContactsGroup? = null
        private set

    var selectionCallbackCount: Int = 0
        private set

    override fun onContactsGroupSelected(group: ContactsGroup?) {
        selectedGroup = group
        selectionCallbackCount++
    }
}
