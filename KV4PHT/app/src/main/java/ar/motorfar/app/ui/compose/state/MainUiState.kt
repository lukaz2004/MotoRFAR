package ar.motorfar.app.ui.compose.state

import ar.motorfar.app.data.ChannelMemory
import ar.motorfar.app.ui.compose.theme.AppTheme

data class MainUiState(
    val activeFrequency: String         = "139.970",
    val activeChannelName: String       = "PRINCIPAL",
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
            activeChannelName = "PRINCIPAL",
            sMeterLevel       = 4,
            isConnected       = false
        )
    }
}

val MainUiState.isEmergencyActive: Boolean
    get() = activeChannelName == "EMERGENCIA"

/** Tono CTCSS del canal activo, formateado para el HUD ("100 Hz"), o null si no tiene. */
val MainUiState.activeToneLabel: String?
    get() {
        val tone = channels.firstOrNull { it.frequency == activeFrequency }?.txTone ?: return null
        val normalized = ar.motorfar.app.ui.ToneHelper.normalizeTone(tone)
        return normalized.takeIf { it != "None" }?.let { "$it Hz" }
    }
