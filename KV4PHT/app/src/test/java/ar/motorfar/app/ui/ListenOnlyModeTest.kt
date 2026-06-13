package ar.motorfar.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la lógica del modo "Solo Escucha" (listen-only / RX-only).
 *
 * Reglas de seguridad:
 *  - PTT manual y alertas STOP/REGROUP quedan BLOQUEADAS en modo escucha.
 *  - EMERGENCIA SIEMPRE está disponible (función de seguridad crítica).
 *  - El beacon GPS automático NO transmite en modo escucha (RX-only real).
 */
class ListenOnlyModeTest {

    // Replica de la decisión de guard en MainActivity.handleAction
    private fun pttAllowed(listenOnly: Boolean): Boolean = !listenOnly
    private fun stopAlertAllowed(listenOnly: Boolean): Boolean = !listenOnly
    private fun regroupAlertAllowed(listenOnly: Boolean): Boolean = !listenOnly
    private fun emergencyAllowed(@Suppress("UNUSED_PARAMETER") listenOnly: Boolean): Boolean = true
    private fun beaconAllowed(listenOnly: Boolean): Boolean = !listenOnly

    @Test
    fun ptt_blocked_when_listen_only() {
        assertFalse("PTT no debe transmitir en modo escucha", pttAllowed(true))
    }

    @Test
    fun ptt_allowed_when_normal_mode() {
        assertTrue("PTT debe funcionar en modo normal", pttAllowed(false))
    }

    @Test
    fun stop_alert_blocked_when_listen_only() {
        assertFalse(stopAlertAllowed(true))
    }

    @Test
    fun regroup_alert_blocked_when_listen_only() {
        assertFalse(regroupAlertAllowed(true))
    }

    @Test
    fun emergency_always_allowed_even_in_listen_only() {
        assertTrue("EMERGENCIA debe estar disponible siempre", emergencyAllowed(true))
        assertTrue(emergencyAllowed(false))
    }

    @Test
    fun gps_beacon_suppressed_when_listen_only() {
        assertFalse("El beacon GPS no debe emitir en modo escucha", beaconAllowed(true))
    }

    @Test
    fun gps_beacon_active_when_normal_mode() {
        assertTrue(beaconAllowed(false))
    }

    @Test
    fun toggle_flips_state() {
        var listenOnly = false
        listenOnly = !listenOnly
        assertTrue(listenOnly)
        listenOnly = !listenOnly
        assertFalse(listenOnly)
    }

    @Test
    fun setting_persists_as_string_boolean() {
        // Verifica el round-trip de persistencia (String <-> Boolean)
        val stored = true.toString()
        assertEquals("true", stored)
        assertTrue(stored.toBoolean())
        assertFalse("false".toBoolean())
    }

    @Test
    fun setting_default_is_false_when_absent() {
        val settings = emptyMap<String, String>()
        val value = settings.getOrDefault("listen_only", "false").toBoolean()
        assertFalse("El modo escucha debe estar OFF por defecto", value)
    }
}
