package com.vagell.kv4pht.ui

import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AlertTypeTest {

    @Test
    fun all_alert_types_exist() {
        val types = AlertHelper.AlertType.values()
        assertTrue(types.size >= 3)
    }

    @Test
    fun emergency_type_exists() {
        assertNotNull(AlertHelper.AlertType.EMERGENCY)
    }

    @Test
    fun stop_type_exists() {
        assertNotNull(AlertHelper.AlertType.STOP)
    }

    @Test
    fun regroup_type_exists() {
        assertNotNull(AlertHelper.AlertType.REGROUP)
    }

    @Test
    fun emergency_message_contains_alerta_keyword() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.EMERGENCY, "LUKAZ")
        assertTrue(msg.contains("ALERTA"))
    }

    @Test
    fun stop_message_contains_detencion_keyword() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.STOP, "LUKAZ")
        assertTrue(msg.contains("DETENCION"))
    }

    @Test
    fun regroup_message_contains_reagrupamiento_keyword() {
        val msg = AlertHelper.buildMessage(AlertHelper.AlertType.REGROUP, "LUKAZ")
        assertTrue(msg.contains("REAGRUPAMIENTO"))
    }
}
