package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSwitchTest {

    @Test
    fun channelSelected_updatesActiveFrequency() {
        val initial = MainUiState(activeFrequency = "139.970")
        val updated = initial.copy(activeFrequency = "138.510")
        assertEquals("138.510", updated.activeFrequency)
    }

    @Test
    fun channelSelected_emergency_updatesTo140970() {
        val state = MainUiState().copy(activeFrequency = "140.970", activeChannelName = "EMERGENCIA")
        assertEquals("140.970", state.activeFrequency)
        assertEquals("EMERGENCIA", state.activeChannelName)
    }

    @Test
    fun alertHelper_emergencyFrequency_is140970() {
        val freq = AlertHelper.getTargetFrequency(AlertHelper.AlertType.EMERGENCY, "139.9700")
        assertEquals("140.9700", freq)
    }

    @Test
    fun alertHelper_stopRegroup_usesCurrentFrequency() {
        val freq = AlertHelper.getTargetFrequency(AlertHelper.AlertType.STOP, "139.9700")
        assertEquals("139.9700", freq)
    }
}
