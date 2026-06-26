package com.github.yuukis.businessmap.app

import android.Manifest
import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import java.io.FileInputStream

internal object TestPermissions {

    /**
     * MainActivity.requestMissingPermissions() pops a system permission
     * dialog for any of these that are missing, which steals window focus
     * and makes any ComposeView shown on top of it undiscoverable by
     * Compose Test. Granting them up front keeps MainActivity resumed and
     * focused.
     *
     * Uses UiAutomation.grantRuntimePermission() where available rather than
     * shelling out to `pm grant`: shell commands operate on the live,
     * currently-instrumented app process from the outside. API 27 does not
     * have that UiAutomation method, so only those old devices fall back to
     * `pm grant`.
     */
    fun grantContactsAndLocation() {
        forEachPermission { permission ->
            grantRuntimePermission(permission)
        }
    }

    private fun grantRuntimePermission(permission: String) {
        val packageName = packageName()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            uiAutomation().grantRuntimePermission(packageName, permission)
        } else {
            executeShellCommand("pm grant $packageName $permission")
        }
    }

    private fun executeShellCommand(command: String) {
        uiAutomation().executeShellCommand(command).use { descriptor ->
            FileInputStream(descriptor.fileDescriptor).use { stream ->
                stream.readBytes()
            }
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
