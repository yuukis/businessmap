package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class LicenseDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    @Test
    fun showsTitleAndAtLeastOneLicense() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                LicenseDialogFragment.showDialog(activity)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(targetContext.getString(R.string.title_licenses)).assertExists()
            composeTestRule.onNodeWithTag("license_item_0").assertExists()
        }
    }

    @Test
    fun clickingItemShowsDetailAndBackReturnsToList() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                LicenseDialogFragment.showDialog(activity)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithTag("license_item_0").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("license_detail").assertExists()

            composeTestRule.onNodeWithTag("license_nav_icon").performClick()
            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithTag("license_item_0").assertExists()
        }
    }
}
