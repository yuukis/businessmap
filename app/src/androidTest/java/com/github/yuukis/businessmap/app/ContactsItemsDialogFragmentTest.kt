package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.yuukis.businessmap.model.ContactsItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsItemsDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    private fun newContactsList() = listOf(
        ContactsItem(
            cid = 1L,
            name = "Taro Yamada",
            phonetic = null,
            groupId = 0L,
            address = "Tokyo",
            note = null,
            companyName = "ACME"
        ),
        ContactsItem(
            cid = 2L,
            name = "Hanako Suzuki",
            phonetic = null,
            groupId = 0L,
            address = "Osaka",
            note = null,
            companyName = "Beta"
        )
    )

    @Test
    fun showsAllContactsAndSelectingOneDismissesTheDialog() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                ContactsItemsDialogFragment.showDialog(activity, newContactsList())
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Taro Yamada").assertExists()
            composeTestRule.onNodeWithText("Hanako Suzuki").assertExists()

            composeTestRule.onNodeWithText("Taro Yamada").performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Taro Yamada").assertDoesNotExist()
        }
    }
}
