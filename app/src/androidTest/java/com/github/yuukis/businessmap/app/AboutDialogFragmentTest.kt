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
class AboutDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    @Test
    fun showsAppNameAndProvider() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AboutDialogFragment.showDialog(activity)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(targetContext.getString(R.string.app_name)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.provider)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.label_licenses)).assertExists()
        }
    }

    @Test
    fun clickingLicensesOpensLicenseDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AboutDialogFragment.showDialog(activity)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(targetContext.getString(R.string.label_licenses)).performClick()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            // LicenseDialogFragment is a separate Compose dialog stacked on top of
            // AboutDialogFragment; AboutDialogFragment is not dismissed underneath it.
            // Both dialogs' titles share the same text ("Open source licenses"), so the
            // list's testTag is used instead of text to identify the new dialog uniquely.
            composeTestRule.onNodeWithTag("license_list").assertExists()
        }
    }
}
