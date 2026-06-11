package com.vagell.kv4pht.ui

import com.vagell.kv4pht.data.ArgentinaChannels
import com.vagell.kv4pht.ui.compose.state.MainUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class ChannelSelectorTest {

    private fun selectChannel(state: MainUiState, freq: String): MainUiState {
        val channels = ArgentinaChannels.getAll()
        val name = channels.firstOrNull { it.frequency == freq }?.name ?: "SIMPLEX"
        return state.copy(activeFrequency = freq, activeChannelName = name)
    }

    @Test
    fun selecting_alternativo_updates_active_channel() {
        val initial = MainUiState()
        val updated = selectChannel(initial, "138.5100")
        assertEquals("ALTERNATIVO", updated.activeChannelName)
        assertEquals("138.5100", updated.activeFrequency)
    }

    @Test
    fun selecting_grupo_updates_active_channel() {
        val initial = MainUiState(activeFrequency = "138.5100", activeChannelName = "ALTERNATIVO")
        val updated = selectChannel(initial, "139.9700")
        assertEquals("GRUPO", updated.activeChannelName)
        assertEquals("139.9700", updated.activeFrequency)
    }

    @Test
    fun selecting_emergencia_updates_active_channel() {
        val initial = MainUiState()
        val updated = selectChannel(initial, "140.9700")
        assertEquals("EMERGENCIA", updated.activeChannelName)
        assertEquals("140.9700", updated.activeFrequency)
    }

    @Test
    fun unknown_frequency_falls_back_to_simplex() {
        val initial = MainUiState()
        val updated = selectChannel(initial, "145.0000")
        assertEquals("SIMPLEX", updated.activeChannelName)
        assertEquals("145.0000", updated.activeFrequency)
    }
}
