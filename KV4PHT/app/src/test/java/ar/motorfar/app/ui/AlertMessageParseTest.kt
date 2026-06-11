package ar.motorfar.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AlertMessageParseTest {

    private fun parseAlertType(body: String): AlertHelper.AlertType? = when {
        body.contains("ALERTA")         -> AlertHelper.AlertType.EMERGENCY
        body.contains("DETENCION")      -> AlertHelper.AlertType.STOP
        body.contains("REAGRUPAMIENTO") -> AlertHelper.AlertType.REGROUP
        else                            -> null
    }

    @Test
    fun emergency_message_is_detected() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.EMERGENCY, "LUKAZ")
        assertEquals(AlertHelper.AlertType.EMERGENCY, parseAlertType(msg))
    }

    @Test
    fun stop_message_is_detected() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.STOP, "LUKAZ")
        assertEquals(AlertHelper.AlertType.STOP, parseAlertType(msg))
    }

    @Test
    fun regroup_message_is_detected() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.REGROUP, "LUKAZ")
        assertEquals(AlertHelper.AlertType.REGROUP, parseAlertType(msg))
    }

    @Test
    fun position_beacon_is_not_detected_as_alert() {
        val msg = "!-34.5678N/058.4321W>"
        assertNull(parseAlertType(msg))
    }

    @Test
    fun random_chat_is_not_detected_as_alert() {
        val msg = "Hola grupo, donde están?"
        assertNull(parseAlertType(msg))
    }

    @Test
    fun emergency_message_includes_callsign() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.EMERGENCY, "LUKAZ")
        assertNotNull(msg)
        assert(msg.contains("LUKAZ"))
    }
}
