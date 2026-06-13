package ar.motorfar.app.ui.compose.state

/**
 * Mensaje de chat VHF (texto vía APRS sobre la radio).
 *
 * Si [alertType] no es null, el mensaje es una alerta de grupo y se
 * renderiza con color y borde según su severidad (EMERGENCY/STOP/REGROUP).
 */
data class ChatMessage(
    val id: Long,
    val fromAlias: String,
    val text: String,
    val timestampMs: Long,
    val isOutgoing: Boolean,
    val alertType: AlertKind? = null,
    val lat: Double? = null,
    val lon: Double? = null
) {
    enum class AlertKind { EMERGENCY, STOP, REGROUP }
}
