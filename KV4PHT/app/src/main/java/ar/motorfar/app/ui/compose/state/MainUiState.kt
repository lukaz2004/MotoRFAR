package ar.motorfar.app.ui.compose.state

import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.ui.compose.theme.AppTheme

data class MainUiState(
    val activeFrequency: String         = "139.970",
    val activeChannelName: String       = "GRUPO",
    val channels: List<ChannelMemory>   = emptyList(),
    val sMeterLevel: Int                = 0,
    val isTxActive: Boolean             = false,
    val isRxActive: Boolean             = false,
    val isConnected: Boolean            = false,
    val theme: AppTheme                 = AppTheme.GREEN,
    val activeAlert: ReceivedAlert?     = null,
    val isListenOnly: Boolean           = false,
    val locationGranted: Boolean        = false,
    val headingDeg: Float?              = null,
    val isRouteActive: Boolean          = false,
    val alertHistory: List<ReceivedAlert> = emptyList(),
    val fallCountdown: Int?             = null
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
