package ar.motorfar.app.ui.compose.state

/**
 * Mensaje de chat VHF (texto vía APRS sobre la radio).
 */
data class ChatMessage(
    val id: Long,
    val fromAlias: String,
    val text: String,
    val timestampMs: Long,
    val isOutgoing: Boolean
)
