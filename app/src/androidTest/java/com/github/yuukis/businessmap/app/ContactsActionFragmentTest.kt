package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ContactsActionFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

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
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_contacts_detail)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_directions)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_drive_navigation)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_street_view)).assertExists()
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

            val showContactsLabel = targetContext.getString(R.string.action_contacts_detail)
            composeTestRule.onNodeWithText(showContactsLabel).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(showContactsLabel).assertDoesNotExist()
        }
    }
}
