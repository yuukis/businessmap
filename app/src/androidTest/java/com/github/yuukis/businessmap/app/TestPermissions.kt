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
     *
     * Uses UiAutomation.grantRuntimePermission()/revokeRuntimePermission()
     * rather than shelling out to `pm grant`/`pm revoke`: those shell
     * commands operate on the live, currently-instrumented app process from
     * the outside and can crash it (the platform itself warns "is more
     * robust and should be used instead of 'pm revoke'" when you do this).
     */
    fun grantContactsAndLocation() {
        forEachPermission { permission ->
            uiAutomation().grantRuntimePermission(packageName(), permission)
        }
    }

    /**
     * See grantContactsAndLocation(). Call this from @After so each test
     * leaves the process in a clean state for whichever test runs next.
     */
    fun revokeContactsAndLocation() {
        forEachPermission { permission ->
            uiAutomation().revokeRuntimePermission(packageName(), permission)
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
