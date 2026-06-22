package com.github.yuukis.businessmap.app

import android.Manifest
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsGroupDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun grantContactsPermission() {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .executeShellCommand("pm grant $packageName ${Manifest.permission.READ_CONTACTS}")
            .close()
    }

    @Test
    fun showsAllContactsGroupAndSelectingItFinishesTheActivity() {
        ActivityScenario.launch(IncomingShortcutActivity::class.java).use { scenario ->
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("All contacts").assertExists()
            composeTestRule.onNodeWithText("All contacts").performClick()
            composeTestRule.waitForIdle()

            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
