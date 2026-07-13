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
    // 2026-07-13: distinto de isConnected -- isConnected exige handshake
    // completo CON modulo de radio encontrado (nunca se pone true sin SA818
    // fisico). Cambiar SSID/clave WiFi es un comando que el firmware procesa
    // sin importar el estado del modulo de radio -- solo necesita el enlace
    // WiFi/UDP arriba (Hello recibido). Gatear esos botones en isConnected
    // los dejaba inutilizables en un equipo sin SA818.
    val isWifiLinkUp: Boolean           = false,
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
