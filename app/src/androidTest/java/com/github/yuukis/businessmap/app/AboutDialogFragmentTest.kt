package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AboutDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Test
    fun showsAppNameAndProvider() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AboutDialogFragment.showDialog(activity)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Business Map").assertExists()
            composeTestRule.onNodeWithText("Yuuki Shimizu").assertExists()
            composeTestRule.onNodeWithText("Open source licenses").assertExists()
        }
    }

    @Test
    fun clickingLicensesOpensLicenseDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                AboutDialogFragment.showDialog(activity)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Open source licenses").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Business Map").assertDoesNotExist()
        }
    }
}
