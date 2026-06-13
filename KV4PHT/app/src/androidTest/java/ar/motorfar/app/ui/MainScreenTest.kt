package ar.motorfar.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.ui.compose.MainScreen
import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun launchMainScreen(state: MainUiState = MainUiState.preview()) {
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(state = state, onAction = {})
            }
        }
    }

    @Test
    fun mainScreen_showsChannelName() {
        launchMainScreen()
        composeTestRule.onNodeWithText("GRUPO").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsActiveFrequency() {
        launchMainScreen()
        composeTestRule.onNodeWithText("139.970").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsPttButton() {
        launchMainScreen()
        composeTestRule.onNodeWithText("PTT").assertIsDisplayed()
    }

    @Test
    fun mainScreen_showsEmergencyButton() {
        launchMainScreen()
        composeTestRule.onNodeWithText("EMERGENCIA").assertIsDisplayed()
    }

    @Test
    fun channelSelector_shows_three_channels() {
        val channels = listOf(
            ChannelMemory().apply { name = "GRUPO";       frequency = "139.9700" },
            ChannelMemory().apply { name = "ALTERNATIVO"; frequency = "138.5100" },
            ChannelMemory().apply { name = "EMERGENCIA";  frequency = "140.9700" }
        )
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                MainScreen(
                    state    = MainUiState.preview().copy(channels = channels),
                    onAction = {}
                )
            }
        }
        composeTestRule.onNodeWithText("GRUPO").assertIsDisplayed()
        composeTestRule.onNodeWithText("ALTERNATIVO").assertIsDisplayed()
        composeTestRule.onNodeWithText("EMERGENCIA").assertIsDisplayed()
    }
}
