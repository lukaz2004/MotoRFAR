package com.vagell.kv4pht.ui.compose.state

import com.vagell.kv4pht.data.ChannelMemory
import com.vagell.kv4pht.ui.compose.theme.AppTheme

data class MainUiState(
    val activeFrequency: String         = "139.970",
    val activeChannelName: String       = "GRUPO",
    val channels: List<ChannelMemory>   = emptyList(),
    val sMeterLevel: Int                = 0,
    val isTxActive: Boolean             = false,
    val isRxActive: Boolean             = false,
    val isConnected: Boolean            = false,
    val theme: AppTheme                 = AppTheme.GREEN
) {
    companion object {
        fun preview() = MainUiState(
            activeFrequency   = "139.970",
            activeChannelName = "GRUPO",
            sMeterLevel       = 4,
            isConnected       = false
        )
    }
}
