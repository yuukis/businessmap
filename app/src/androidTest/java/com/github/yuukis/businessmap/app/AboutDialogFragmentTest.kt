package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import org.junit.Assert.assertTrue
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

            composeTestRule.onNodeWithText(targetContext.getString(R.string.label_licenses))
                .performScrollTo()
                .performClick()
            scenario.onActivity { activity ->
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            scenario.onActivity { activity ->
                assertTrue(
                    activity.supportFragmentManager.fragments.any { fragment ->
                        fragment is LicenseDialogFragment && fragment.isAdded
                    }
                )
            }
        }
    }
}
