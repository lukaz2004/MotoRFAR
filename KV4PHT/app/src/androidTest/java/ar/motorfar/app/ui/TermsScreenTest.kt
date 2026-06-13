package ar.motorfar.app.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import ar.motorfar.app.ui.compose.theme.AppTheme
import ar.motorfar.app.ui.compose.theme.MotoRFARTheme
import ar.motorfar.app.ui.onboarding.TermsScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TermsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun aceptar_button_calls_callback() {
        var accepted = false
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                TermsScreen(step = 1, onAccept = { accepted = true })
            }
        }
        composeTestRule.onNodeWithText("ACEPTAR").performClick()
        assertTrue(accepted)
    }

    @Test
    fun terms_screen_shows_resolution_reference() {
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                TermsScreen(step = 1, onAccept = {})
            }
        }
        composeTestRule.onNodeWithText("Resolución 5/2015", substring = true).assertExists()
    }
}
