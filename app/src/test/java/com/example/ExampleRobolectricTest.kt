package com.example

import android.content.Context
import android.provider.Settings
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.data.SystemControlManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class ExampleRobolectricTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun `app name and large clock with hours minutes and seconds render on home and lock screen`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("EcoClock", context.getString(R.string.app_name))

        // Verify Home screen and Large Precision Clock displaying hours, minutes, and seconds
        composeTestRule.onNodeWithTag("home_screen_container").assertExists()
        composeTestRule.onNodeWithTag("large_precision_clock").assertExists()
        composeTestRule.onNodeWithTag("clock_hours_minutes_text").assertExists()
        composeTestRule.onNodeWithTag("clock_seconds_text").assertExists()

        // Navigate to System Core settings tab
        composeTestRule.onNodeWithTag("nav_tab_system_core").performClick()
        composeTestRule.onNodeWithTag("system_core_screen").assertExists()

        // Navigate to Lock Screen Standby and verify clock renders over lock screen mode
        composeTestRule.onNodeWithTag("nav_tab_lock_screen").performClick()
        composeTestRule.onNodeWithTag("lock_screen_view").assertExists()
        composeTestRule.onNodeWithTag("clock_seconds_text").assertExists()

        // Unlock back to Home Launcher
        composeTestRule.onNodeWithTag("unlock_to_home_button").performClick()
        composeTestRule.onNodeWithTag("home_screen_container").assertExists()
    }

    @Test
    fun `system control manager updates window brightness and reads core system settings`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SystemControlManager(context)
        var capturedWindowFraction = -1f
        manager.windowBrightnessUpdater = { fraction ->
            capturedWindowFraction = fraction
        }

        manager.setSystemBrightness(51) // 20% of 255
        assertTrue(capturedWindowFraction in 0.19f..0.21f)

        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 51)
        Settings.System.putInt(context.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 15_000)

        val state = manager.readCoreSystemSettings()
        assertEquals(51, state.systemBrightness)
        assertEquals(15_000, state.screenOffTimeoutMs)
    }
}
