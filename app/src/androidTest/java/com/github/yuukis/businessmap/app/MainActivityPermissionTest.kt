package com.github.yuukis.businessmap.app

import android.Manifest
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies MainActivity launches without crashing when the contacts and
 * location permissions have not been granted yet. The runtime permission
 * dialog this triggers takes the foreground, so the activity itself only
 * reaches STARTED rather than RESUMED.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

    @Before
    fun revokeRuntimePermissions() {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val automation = InstrumentationRegistry.getInstrumentation().uiAutomation
        for (permission in listOf(
            Manifest.permission.READ_CONTACTS,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )) {
            automation.executeShellCommand("pm revoke $packageName $permission").close()
        }
    }

    @Test
    fun launchesWithoutCrashingWhenPermissionsAreMissing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(scenario.state.isAtLeast(Lifecycle.State.STARTED))
        }
    }
}
