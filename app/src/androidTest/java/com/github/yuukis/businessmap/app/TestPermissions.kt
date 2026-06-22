package com.github.yuukis.businessmap.app

import android.Manifest
import androidx.test.platform.app.InstrumentationRegistry

internal object TestPermissions {

    /**
     * MainActivity.requestMissingPermissions() pops a system permission
     * dialog for any of these that are missing, which steals window focus
     * and makes any ComposeView shown on top of it undiscoverable by
     * Compose Test. Granting them up front keeps MainActivity resumed and
     * focused.
     */
    fun grantContactsAndLocation() {
        forEachPermission { permission ->
            uiAutomation().executeShellCommand("pm grant ${packageName()} $permission").close()
        }
    }

    /**
     * Revoking a dangerous permission from a process that is still holding
     * resources backed by it (an open ContentProvider connection, a location
     * callback, etc.) can make the platform kill that process to enforce the
     * revoke. Since androidTest classes share one app process for the whole
     * instrumentation run, leaving these granted after a test that actually
     * exercised them (e.g. ContactsGroupDialogFragmentTest querying
     * contacts) can crash a later, unrelated test (e.g.
     * MainActivityPermissionTest) when IT revokes the same permissions.
     * Call this from @After so each test leaves the process in a clean
     * state for whichever test runs next.
     */
    fun revokeContactsAndLocation() {
        forEachPermission { permission ->
            uiAutomation().executeShellCommand("pm revoke ${packageName()} $permission").close()
        }
    }

    private fun forEachPermission(action: (String) -> Unit) {
        for (permission in listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )) {
            action(permission)
        }
    }

    private fun packageName() = InstrumentationRegistry.getInstrumentation().targetContext.packageName
    private fun uiAutomation() = InstrumentationRegistry.getInstrumentation().uiAutomation
}
