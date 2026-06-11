package ar.motorfar.app.ui

import org.junit.Assert.assertTrue
import org.junit.Test

class ToneHelperTest {

    @Test
    fun playPttDown_returns_within_100ms() {
        val start = System.currentTimeMillis()
        ToneHelper.playPttDown(0.7f)
        assertTrue(System.currentTimeMillis() - start < 100L)
    }

    @Test
    fun playPttUp_returns_within_100ms() {
        val start = System.currentTimeMillis()
        ToneHelper.playPttUp(0.7f)
        assertTrue(System.currentTimeMillis() - start < 100L)
    }

    @Test
    fun playAlertBeep_returns_within_500ms() {
        val start = System.currentTimeMillis()
        ToneHelper.playAlertBeep(0.7f)
        assertTrue(System.currentTimeMillis() - start < 500L)
    }

    @Test
    fun playEmergencyBeep_returns_within_1000ms() {
        val start = System.currentTimeMillis()
        ToneHelper.playEmergencyBeep(0.7f)
        assertTrue(System.currentTimeMillis() - start < 1000L)
    }
}
