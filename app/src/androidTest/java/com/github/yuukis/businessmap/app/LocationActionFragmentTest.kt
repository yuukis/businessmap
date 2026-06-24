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
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasPackage
import androidx.test.platform.app.InstrumentationRegistry
import com.github.yuukis.businessmap.R
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LocationActionFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val targetContext get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val lat = 35.681236
    private val lng = 139.767125
    private val address = "Tokyo"

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    @Test
    fun showsAddressAndActionItems() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                LocationActionFragment.showDialog(activity, lat, lng, address)
                activity.supportFragmentManager.executePendingTransactions()
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText(address).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_register_contact)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_directions)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_drive_navigation)).assertExists()
            composeTestRule.onNodeWithText(targetContext.getString(R.string.action_street_view)).assertExists()
        }
    }

    @Test
    fun clickingRegisterContactStartsContactInsertIntent() {
        verifyActionIntent(
            R.string.action_register_contact,
            allOf(
                hasAction(Intent.ACTION_INSERT),
                hasData(ContactsContract.Contacts.CONTENT_URI),
                hasExtra(ContactsContract.Intents.Insert.POSTAL, address)
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
                hasData(Uri.parse("google.navigation:///?ll=35.681236,139.767125&q=Tokyo")),
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
                    LocationActionFragment.showDialog(activity, lat, lng, address)
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
