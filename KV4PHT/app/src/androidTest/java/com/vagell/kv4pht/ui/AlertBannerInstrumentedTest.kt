package com.vagell.kv4pht.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vagell.kv4pht.ui.AlertHelper
import com.vagell.kv4pht.ui.compose.components.AlertBanner
import com.vagell.kv4pht.ui.compose.state.ReceivedAlert
import com.vagell.kv4pht.ui.compose.theme.AppTheme
import com.vagell.kv4pht.ui.compose.theme.MotoRFARTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AlertBannerInstrumentedTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun alert_banner_hidden_on_start() {
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                AlertBanner(alert = null, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithTag("alert_banner").assertDoesNotExist()
    }

    @Test
    fun alert_banner_shown_when_active() {
        val alert = ReceivedAlert(
            type         = AlertHelper.AlertType.STOP,
            fromAlias    = "MOTO1",
            receivedAtMs = 0L
        )
        composeTestRule.setContent {
            MotoRFARTheme(AppTheme.GREEN) {
                AlertBanner(alert = alert, onDismiss = {})
            }
        }
        composeTestRule.onNodeWithTag("alert_banner").assertIsDisplayed()
    }
}
