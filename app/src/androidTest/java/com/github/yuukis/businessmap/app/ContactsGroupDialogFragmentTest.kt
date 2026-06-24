package com.github.yuukis.businessmap.app

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsGroup
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

    @Test
    fun showsAllContactsGroupAndSelectingItNotifiesListenerAndDismissesTheDialog() {
        ActivityScenario.launch(FakeContactsGroupSelectListenerActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                ContactsGroupDialogFragment.showDialog(activity)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            val allContactsLabel = targetContext.getString(R.string.group_all_contacts)
            composeTestRule.onNodeWithText(allContactsLabel).assertExists()
            composeTestRule.onNodeWithText(allContactsLabel).performClick()
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(allContactsLabel).assertDoesNotExist()

            scenario.onActivity { activity ->
                assertEquals(1, activity.selectionCallbackCount)
                assertEquals(ContactsGroup.ID_GROUP_ALL_CONTACTS, activity.selectedGroup?.id)
            }
        }
    }
}
