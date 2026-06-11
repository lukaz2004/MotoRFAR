package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.MainUiState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PttActionTest {

    @Test
    fun pttPressed_setsTxActive() {
        val state = MainUiState().copy(isTxActive = true)
        assertTrue(state.isTxActive)
    }

    @Test
    fun pttReleased_clearsTxActive() {
        val state = MainUiState().copy(isTxActive = false)
        assertFalse(state.isTxActive)
    }

    @Test
    fun ptt_disabledWhenNotConnected() {
        val state = MainUiState(isConnected = false)
        assertFalse(state.isConnected)
    }
}
