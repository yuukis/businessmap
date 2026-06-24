package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationActionFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    @Test
    fun showsAddressAndActionItems() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                LocationActionFragment.showDialog(activity, 35.0, 139.0, "Tokyo")
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Tokyo").assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_register_contact)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_directions)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_drive_navigation)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_street_view)).assertExists()
        }
    }

    @Test
    fun clickingRegisterContactDismissesTheDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                LocationActionFragment.showDialog(activity, 35.0, 139.0, "Tokyo")
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            val registerContactLabel = targetContext.getString(R.string.action_register_contact)
            composeTestRule.onNodeWithText(registerContactLabel).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(registerContactLabel).assertDoesNotExist()
        }
    }
}
