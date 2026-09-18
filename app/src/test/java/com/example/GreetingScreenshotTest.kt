package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.model.TunnelConfig
import com.example.model.TunnelState
import com.example.model.TunnelStats
import com.example.ui.components.HnTopBar
import com.example.ui.screens.HomeScreen
import com.example.ui.theme.HnTunnelTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class GreetingScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun greeting_screenshot() {
    composeTestRule.setContent {
      HnTunnelTheme {
        HomeScreen(
          config = TunnelConfig(name = "Cloudflare SSL/SNI"),
          state = TunnelState.DISCONNECTED,
          stats = TunnelStats(),
          onToggleConnect = {},
          onConfigChange = {},
          onOpenPayloadGen = {}
        )
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }
}
