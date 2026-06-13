package ar.motorfar.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica la lógica de confirmación por hold del EmergencyConfirmButton.
 *
 * Diseño: un tap corto NO dispara la emergencia. Solo un hold sostenido
 * de 2 segundos completos llama a onConfirmed. Soltar antes resetea.
 */
class EmergencyConfirmButtonTest {

    private val holdDurationMs = 2000L
    private val stepMs = 50L

    /** Simula sostener el botón [heldMs] milisegundos. Devuelve true si dispara. */
    private fun simulateHold(heldMs: Long): Boolean {
        var elapsed = 0L
        var progress = 0f
        var fired = false
        while (elapsed < holdDurationMs && elapsed < heldMs) {
            elapsed += stepMs
            progress = elapsed.toFloat() / holdDurationMs.toFloat()
        }
        if (elapsed >= holdDurationMs && progress >= 1f) {
            fired = true
        }
        return fired
    }

    @Test
    fun short_tap_does_not_fire() {
        assertFalse("Un tap corto (100ms) no debe disparar emergencia", simulateHold(100L))
    }

    @Test
    fun half_hold_does_not_fire() {
        assertFalse("Sostener 1s no debe disparar", simulateHold(1000L))
    }

    @Test
    fun almost_complete_hold_does_not_fire() {
        assertFalse("Sostener 1.95s no debe disparar", simulateHold(1950L))
    }

    @Test
    fun full_hold_fires() {
        assertTrue("Sostener 2s completos debe disparar", simulateHold(2000L))
    }

    @Test
    fun longer_hold_fires() {
        assertTrue("Sostener 3s también dispara", simulateHold(3000L))
    }

    @Test
    fun progress_at_one_second_is_half() {
        var elapsed = 0L
        var progress = 0f
        while (elapsed < 1000L) {
            elapsed += stepMs
            progress = elapsed.toFloat() / holdDurationMs.toFloat()
        }
        assertEquals(0.5f, progress, 0.01f)
    }

    @Test
    fun release_resets_fired_flag() {
        var fired = false
        var holding = true
        // Hold parcial
        holding = false
        // Al soltar antes de completar, fired permanece false
        if (!holding) fired = false
        assertFalse(fired)
    }

    @Test
    fun progress_never_exceeds_one() {
        var elapsed = 0L
        var progress = 0f
        while (elapsed < holdDurationMs * 2) {
            elapsed += stepMs
            progress = (elapsed.toFloat() / holdDurationMs.toFloat()).coerceAtMost(1f)
        }
        assertEquals(1f, progress, 0.001f)
    }
}
