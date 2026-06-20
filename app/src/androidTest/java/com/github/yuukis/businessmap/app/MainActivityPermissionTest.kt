package com.github.yuukis.businessmap.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Verifies MainActivity launches without crashing when the contacts and
 * location permissions have not been granted yet.
 */
@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

    @Test
    fun launchesWithoutCrashingWhenPermissionsAreMissing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }
}
