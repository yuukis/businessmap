package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.yuukis.businessmap.model.ContactsItem
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsActionFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private fun newContact() = ContactsItem(
        cid = 1L,
        name = "Taro Yamada",
        phonetic = null,
        groupId = 0L,
        address = "Tokyo",
        note = null,
        companyName = "ACME"
    )

    @Test
    fun showsContactNameAndActionItems() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                ContactsActionFragment.showDialog(activity, newContact())
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Taro Yamada").assertExists()
            composeTestRule.onNodeWithText("Show contacts").assertExists()
            composeTestRule.onNodeWithText("Directions").assertExists()
            composeTestRule.onNodeWithText("Drive navigation").assertExists()
        }
    }

    @Test
    fun clickingShowContactsDismissesTheDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                ContactsActionFragment.showDialog(activity, newContact())
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Show contacts").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Show contacts").assertDoesNotExist()
        }
    }
}
