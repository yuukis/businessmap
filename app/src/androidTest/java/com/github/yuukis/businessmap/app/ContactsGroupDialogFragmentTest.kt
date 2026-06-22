package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsGroupDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    @After
    fun revokeRuntimePermissions() {
        TestPermissions.revokeContactsAndLocation()
    }

    @Test
    fun showsAllContactsGroupAndSelectingItFinishesTheActivity() {
        ActivityScenario.launch(IncomingShortcutActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            val allContactsLabel = targetContext.getString(R.string.group_all_contacts)
            composeTestRule.onNodeWithText(allContactsLabel).assertExists()
            composeTestRule.onNodeWithText(allContactsLabel).performClick()

            // finish() drives the activity to DESTROYED asynchronously via the system process;
            // waitForIdle() only settles Compose, so poll instead of asserting immediately.
            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                scenario.state == Lifecycle.State.DESTROYED
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
