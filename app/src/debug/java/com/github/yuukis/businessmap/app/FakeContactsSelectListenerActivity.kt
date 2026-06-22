package com.github.yuukis.businessmap.app

import androidx.fragment.app.FragmentActivity
import com.github.yuukis.businessmap.model.ContactsItem

/**
 * Minimal test-only host for [ContactsItemsDialogFragment] that records the
 * argument passed to [ContactsItemsDialogFragment.OnSelectListener] so tests
 * can verify the callback fired with the expected contact, instead of only
 * observing a side effect (like [MainActivity]'s real implementation, which
 * shows a map marker info window and isn't practical to assert against in a
 * unit-of-behavior test).
 */
class FakeContactsSelectListenerActivity : FragmentActivity(), ContactsItemsDialogFragment.OnSelectListener {

    var selectedContacts: ContactsItem? = null
        private set

    var selectionCallbackCount: Int = 0
        private set

    override fun onContactsSelected(contacts: ContactsItem?) {
        selectedContacts = contacts
        selectionCallbackCount++
    }
}
