package com.github.yuukis.businessmap.app

import android.os.Bundle
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.pressBackUnconditionally
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.fragment.app.FragmentActivity
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ProgressDialogFragmentTest {

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    @Before
    fun grantRuntimePermissions() {
        TestPermissions.grantContactsAndLocation()
    }

    private fun showDialog(activity: FragmentActivity, max: Int): ProgressDialogFragment {
        val fragment = ProgressDialogFragment.newInstance()
        fragment.arguments = Bundle().apply {
            putString(ProgressDialogFragment.TITLE, "Please wait")
            putString(ProgressDialogFragment.MESSAGE, "Converting addresses…")
            putBoolean(ProgressDialogFragment.CANCELABLE, true)
            putInt(ProgressDialogFragment.MAX, max)
        }
        fragment.show(activity.supportFragmentManager, ProgressDialogFragment.TAG)
        activity.supportFragmentManager.executePendingTransactions()
        return fragment
    }

    @Test
    fun showsMessageAndUpdatesProgressCount() {
        ActivityScenario.launch(FakeProgressDialogListenerActivity::class.java).use { scenario ->
            var fragment: ProgressDialogFragment? = null
            scenario.onActivity { activity ->
                fragment = showDialog(activity, max = 10)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("Converting addresses…").assertExists()
            composeTestRule.onNodeWithText("0/10").assertExists()

            scenario.onActivity {
                fragment?.updateProgress(5)
            }
            composeTestRule.waitForIdle()

            composeTestRule.onNodeWithText("5/10").assertExists()
        }
    }

    @Test
    fun cancellingTheDialogNotifiesTheListener() {
        ActivityScenario.launch(FakeProgressDialogListenerActivity::class.java).use { scenario ->
            var fragment: ProgressDialogFragment? = null
            scenario.onActivity { activity ->
                fragment = showDialog(activity, max = 10)
            }
            composeTestRule.waitForIdle()

            scenario.onActivity {
                fragment?.dialog?.cancel()
            }
            composeTestRule.waitForIdle()

            scenario.onActivity { activity ->
                assertEquals(1, activity.cancelledCallbackCount)
            }
        }
    }

    @Test
    fun pressingBackOnProgressDialogFinishesMainActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                showDialog(activity, max = 10)
            }
            composeTestRule.waitForIdle()

            pressBackUnconditionally()

            composeTestRule.waitUntil(timeoutMillis = 5_000) {
                scenario.state == Lifecycle.State.DESTROYED
            }
            assertEquals(Lifecycle.State.DESTROYED, scenario.state)
        }
    }
}
