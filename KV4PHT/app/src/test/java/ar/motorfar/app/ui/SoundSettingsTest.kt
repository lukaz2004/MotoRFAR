package ar.motorfar.app.ui

import org.junit.Test

class SoundSettingsTest {

    @Test
    fun playPttDown_volume0_does_not_crash() {
        ToneHelper.playPttDown(0f)
    }

    @Test
    fun playAlertBeep_volume0_does_not_crash() {
        ToneHelper.playAlertBeep(0f)
    }

    @Test
    fun playEmergencyBeep_volume0_does_not_crash() {
        ToneHelper.playEmergencyBeep(0f)
    }
}
