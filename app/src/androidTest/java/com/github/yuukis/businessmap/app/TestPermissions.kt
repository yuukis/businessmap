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
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        for (permission in listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )) {
            automation.executeShellCommand("pm grant $packageName $permission").close()
        }
    }
}
