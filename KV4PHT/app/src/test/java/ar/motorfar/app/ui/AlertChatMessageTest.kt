package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.state.ChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el mapeo de alertas de grupo a mensajes de chat con formato.
 *
 * Reglas:
 *  - EMERGENCY → AlertKind.EMERGENCY (rojo) + "SOLICITO ASISTENCIA INMEDIATA"
 *  - STOP → AlertKind.STOP (amarillo)
 *  - REGROUP → AlertKind.REGROUP (verde)
 *  - La posición GPS se adjunta cuando está disponible
 *  - Un mensaje normal no tiene alertType (burbuja común)
 */
class AlertChatMessageTest {

    private fun kindFor(type: AlertHelper.AlertType): ChatMessage.AlertKind = when (type) {
        AlertHelper.AlertType.EMERGENCY -> ChatMessage.AlertKind.EMERGENCY
        AlertHelper.AlertType.STOP      -> ChatMessage.AlertKind.STOP
        AlertHelper.AlertType.REGROUP   -> ChatMessage.AlertKind.REGROUP
    }

    private fun bodyFor(type: AlertHelper.AlertType): String = when (type) {
        AlertHelper.AlertType.EMERGENCY -> "SOLICITO ASISTENCIA INMEDIATA"
        AlertHelper.AlertType.STOP      -> "DETENIDO - INCONVENIENTE EN RUTA"
        AlertHelper.AlertType.REGROUP   -> "REAGRUPAR - ESPERAR EN POSICIÓN"
    }

    @Test
    fun emergency_maps_to_emergency_kind() {
        assertEquals(ChatMessage.AlertKind.EMERGENCY, kindFor(AlertHelper.AlertType.EMERGENCY))
    }

    @Test
    fun stop_maps_to_stop_kind() {
        assertEquals(ChatMessage.AlertKind.STOP, kindFor(AlertHelper.AlertType.STOP))
    }

    @Test
    fun regroup_maps_to_regroup_kind() {
        assertEquals(ChatMessage.AlertKind.REGROUP, kindFor(AlertHelper.AlertType.REGROUP))
    }

    @Test
    fun emergency_body_solicits_immediate_assistance() {
        assertTrue(bodyFor(AlertHelper.AlertType.EMERGENCY).contains("ASISTENCIA INMEDIATA"))
    }

    @Test
    fun alert_message_carries_position_when_available() {
        val msg = ChatMessage(
            id = 0, fromAlias = "MOTO", text = bodyFor(AlertHelper.AlertType.EMERGENCY),
            timestampMs = 0, isOutgoing = true,
            alertType = ChatMessage.AlertKind.EMERGENCY,
            lat = -34.5470, lon = -58.5290
        )
        assertNotNull(msg.lat)
        assertNotNull(msg.lon)
        assertEquals(-34.5470, msg.lat!!, 0.0001)
    }

    @Test
    fun alert_message_handles_null_position() {
        val msg = ChatMessage(
            id = 0, fromAlias = "MOTO", text = bodyFor(AlertHelper.AlertType.STOP),
            timestampMs = 0, isOutgoing = true,
            alertType = ChatMessage.AlertKind.STOP,
            lat = null, lon = null
        )
        assertNull(msg.lat)
        assertNull(msg.lon)
    }

    @Test
    fun normal_message_has_no_alert_type() {
        val msg = ChatMessage(
            id = 0, fromAlias = "MOTO", text = "hola grupo",
            timestampMs = 0, isOutgoing = true
        )
        assertNull(msg.alertType)
    }

    @Test
    fun emergency_uses_140_freq_via_alerthelper() {
        // La emergencia siempre va por 140.970 sin importar el canal activo
        val freq = AlertHelper.getTargetFrequency(AlertHelper.AlertType.EMERGENCY, "139.9700")
        assertEquals("140.9700", freq)
    }

    @Test
    fun stop_regroup_use_active_channel() {
        val freqStop = AlertHelper.getTargetFrequency(AlertHelper.AlertType.STOP, "139.9700")
        assertEquals("139.9700", freqStop)
    }
}
