package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.MainUiState
import ar.motorfar.app.ui.compose.state.ReceivedAlert
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlertBannerStateTest {

    @Test
    fun default_state_has_no_active_alert() {
        assertNull(MainUiState().activeAlert)
    }

    @Test
    fun state_with_alert_has_non_null_active_alert() {
        val alert = ReceivedAlert(
            type         = AlertHelper.AlertType.STOP,
            fromAlias    = "PEPE",
            receivedAtMs = 1000L
        )
        val state = MainUiState(activeAlert = alert)
        assertNotNull(state.activeAlert)
    }

    @Test
    fun dismissing_alert_sets_active_alert_to_null() {
        val alert = ReceivedAlert(AlertHelper.AlertType.STOP, "PEPE", 1000L)
        val state = MainUiState(activeAlert = alert)
        val dismissed = state.copy(activeAlert = null)
        assertNull(dismissed.activeAlert)
    }

    @Test
    fun alert_retains_type_and_alias() {
        val alert = ReceivedAlert(AlertHelper.AlertType.EMERGENCY, "LUCAS", 5000L)
        val state = MainUiState(activeAlert = alert)
        val active = state.activeAlert!!
        assert(active.type == AlertHelper.AlertType.EMERGENCY)
        assert(active.fromAlias == "LUCAS")
        assert(active.receivedAtMs == 5000L)
    }

    @Test
    fun new_alert_replaces_previous_alert() {
        val first  = ReceivedAlert(AlertHelper.AlertType.STOP,    "PEPE",  1000L)
        val second = ReceivedAlert(AlertHelper.AlertType.REGROUP, "LUCAS", 2000L)
        val state  = MainUiState(activeAlert = first).copy(activeAlert = second)
        assert(state.activeAlert?.fromAlias == "LUCAS")
        assert(state.activeAlert?.type == AlertHelper.AlertType.REGROUP)
    }
}
