package com.github.yuukis.businessmap.app

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityPermissionTest {

    @Before
    fun revokeRuntimePermissions() {
        TestPermissions.revokeContactsAndLocation()
    }

    @Test
    fun launchesWithoutCrashingWhenPermissionsAreMissing() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            assertTrue(scenario.state.isAtLeast(Lifecycle.State.STARTED))
        }
    }
}
