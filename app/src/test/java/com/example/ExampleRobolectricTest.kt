package com.example

import android.content.Context
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.ui.screens.SettingsScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], manifest = Config.NONE)
class ExampleRobolectricTest {

  @get:Rule
  val composeTestRule = createComposeRule()

  @Test
  fun testSettingsScreenRendering() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    try {
      com.example.ui.theme.ThemeManager.init(context)
    } catch (e: Throwable) {
      println("--- EXCEPTION IN ThemeManager.init ---")
      e.printStackTrace()
      throw e
    }

    try {
      composeTestRule.setContent {
        SettingsScreen(
          isMemoryEnabled = true,
          onMemoryEnabledChanged = {},
          notificationsEnabled = true,
          onNotificationsEnabledChanged = {},
          isCollectiveOptIn = true,
          onCollectiveOptInChanged = {},
          activeThemeName = "Deep Sea",
          onThemeSelected = {},
          onWipeAllUserData = {},
          userName = "Abhay Shah",
          userEmail = "abhay@depthlens.ai"
        )
      }
    } catch (e: Throwable) {
      println("--- EXCEPTION IN SettingsScreen ---")
      e.printStackTrace()
      throw e
    }
    composeTestRule.onRoot().assertExists()
  }
}
