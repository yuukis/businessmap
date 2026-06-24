package com.github.yuukis.businessmap.app

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.anyIntent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent
import androidx.test.espresso.intent.matcher.IntentMatchers.hasData
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import com.github.yuukis.businessmap.model.ContactsItem
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
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
    ).apply {
        setLat(35.681236)
        setLng(139.767125)
    }

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
    fun clickingShowContactsStartsContactViewIntent() {
        verifyActionIntent(
            R.string.action_contacts_detail,
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, "1"))
            )
        )
    }

    @Test
    fun clickingDirectionsStartsMapsDirectionsIntent() {
        verifyActionIntent(
            R.string.action_directions,
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse("http://maps.google.com/maps?saddr=&daddr=35.681236,139.767125"))
            )
        )
    }

    @Test
    fun clickingDriveNavigationStartsGoogleMapsNavigationIntent() {
        verifyActionIntent(
            R.string.action_drive_navigation,
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse("google.navigation:///?ll=35.681236,139.767125&q=Taro Yamada")),
                hasComponent(
                    ComponentName(
                        "com.google.android.apps.maps",
                        "com.google.android.maps.driveabout.app.NavigationActivity"
                    )
                )
            )
        )
    }

    @Test
    fun clickingStreetViewStartsGoogleMapsStreetViewIntent() {
        verifyActionIntent(
            R.string.action_street_view,
            allOf(
                hasAction(Intent.ACTION_VIEW),
                hasData(Uri.parse("google.streetview:cbll=35.681236,139.767125")),
                hasPackage("com.google.android.apps.maps")
            )
        )
    }

    private fun verifyActionIntent(labelId: Int, intentMatcher: Matcher<Intent>) {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            Intents.init()
            try {
                intending(anyIntent()).respondWith(ActivityResult(Activity.RESULT_OK, null))

                scenario.onActivity { activity ->
                    ContactsActionFragment.showDialog(activity, newContact())
                    activity.supportFragmentManager.executePendingTransactions()
                }
                composeTestRule.waitForIdle()

                composeTestRule.onNodeWithText(targetContext.getString(labelId)).performClick()
                composeTestRule.waitForIdle()

                intended(intentMatcher)
            } finally {
                Intents.release()
            }
        }
    }
}
