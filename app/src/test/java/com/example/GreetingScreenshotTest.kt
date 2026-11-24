package com.example

import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.theme.MyApplicationTheme
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
    composeTestRule.setContent { MyApplicationTheme { Greeting("Robolectric") } }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/greeting.png")
  }

  @Test
  fun testMainScreenLaunch() {
    val application = androidx.test.core.app.ApplicationProvider.getApplicationContext<android.content.Context>() as android.app.Application
    val database = com.example.data.local.HistoryDatabase.getDatabase(application)
    val repository = com.example.data.repository.HsCodeRepository(application, database.historyDao())
    val viewModel = com.example.ui.viewmodel.HsCodeViewModel(application, repository)
    
    composeTestRule.setContent {
      MyApplicationTheme {
        com.example.ui.screens.MainScreen(viewModel)
      }
    }
    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/mainscreen_launch.png")
  }
}

@Composable
fun Greeting(name: String, modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier) {
  androidx.compose.material3.Text(text = "Hello $name!", modifier = modifier)
}
