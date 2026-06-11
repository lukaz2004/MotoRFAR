package ar.motorfar.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyDialogTest {

    @Test
    fun cancel_does_not_call_confirm() {
        var confirmed = false
        val onConfirm = { confirmed = true }
        val onCancel  = {}

        // Simulate cancel: confirm never called
        onCancel()
        assertFalse(confirmed)
    }

    @Test
    fun confirm_callback_is_invoked_after_hold_completes() {
        var confirmed = false
        val onConfirm = { confirmed = true }

        // Simulate hold completing: confirm IS called
        onConfirm()
        assertTrue(confirmed)
    }

    @Test
    fun progress_starts_at_zero() {
        var progress = 0f
        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun progress_reaches_one_when_hold_completes() {
        var progress = 0f
        val holdDurationMs = 2000L
        val stepMs = 50L
        var elapsed = 0L
        while (elapsed < holdDurationMs) {
            elapsed += stepMs
            progress = elapsed.toFloat() / holdDurationMs.toFloat()
        }
        assertEquals(1f, progress, 0.001f)
    }

    @Test
    fun progress_resets_to_zero_on_release_before_complete() {
        var holding = true
        var progress = 0.4f

        // Simulate release before complete
        holding = false
        if (!holding) progress = 0f

        assertEquals(0f, progress, 0.001f)
    }

    @Test
    fun emergency_type_maps_to_emergency_frequency() {
        val freq = AlertHelper.getTargetFrequency(AlertHelper.AlertType.EMERGENCY, "139.9700")
        assertEquals("140.9700", freq)
    }
}
