package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class SMeterUpdateTest {

    @Test
    fun sMeterUpdate_setsLevelCorrectly() {
        val initial = MainUiState()
        val updated = initial.copy(sMeterLevel = 7)
        assertEquals(7, updated.sMeterLevel)
    }

    @Test
    fun sMeterUpdate_clampsToMaxNine() {
        val state = MainUiState().copy(sMeterLevel = 9)
        assertEquals(9, state.sMeterLevel)
    }

    @Test
    fun sMeterUpdate_zeroOnNoSignal() {
        val state = MainUiState().copy(sMeterLevel = 0)
        assertEquals(0, state.sMeterLevel)
    }
}
