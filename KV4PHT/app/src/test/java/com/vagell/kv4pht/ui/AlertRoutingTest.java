/*
MotoRFAR — Tests de routing de alertas (Fase C)
Verifica que EMERGENCY vaya siempre a 140.970 MHz y STOP/REGROUP al canal activo.
License: GNU GPL v3
*/

package com.vagell.kv4pht.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.vagell.kv4pht.radio.TxWhitelist;

import org.junit.Test;

/**
 * Verifica la lógica de routing de alertas:
 *
 * - EMERGENCY → 140.970 MHz SIEMPRE (canal exclusivo emergencias Res. 5/2015)
 * - STOP / REGROUP → canal activo del usuario
 *
 * Si el canal activo ya es 140.970, EMERGENCY no hace switch innecesario.
 */
public class AlertRoutingTest {

    @Test
    public void emergency_alwaysRoutesToEmergencyFreq() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.EMERGENCY, "139.9700");
        assertEquals("140.9700", result);
    }

    @Test
    public void emergency_fromAlternativo_routesToEmergencyFreq() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.EMERGENCY, "138.5100");
        assertEquals("140.9700", result);
    }

    @Test
    public void emergency_alreadyOnEmergencyFreq_noSwitch() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.EMERGENCY, "140.9700");
        assertEquals("140.9700", result);
    }

    @Test
    public void stop_staysOnCurrentFreq() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.STOP, "139.9700");
        assertEquals("139.9700", result);
    }

    @Test
    public void stop_fromAlternativo_staysOnAlternativo() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.STOP, "138.5100");
        assertEquals("138.5100", result);
    }

    @Test
    public void regroup_staysOnCurrentFreq() {
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.REGROUP, "139.9700");
        assertEquals("139.9700", result);
    }

    @Test
    public void emergencyFreq_matchesTxWhitelistEmergencyFreq() {
        TxWhitelist whitelist = new TxWhitelist();
        String expected = String.format(java.util.Locale.US, "%.4f", whitelist.getEmergencyFreq());
        String result = AlertHelper.getTargetFrequency(
            AlertHelper.AlertType.EMERGENCY, "139.9700");
        assertEquals(expected, result);
    }

    @Test
    public void emergencyConfirmationText_mentionsEmergencyFreq() {
        String text = AlertHelper.getConfirmationText(AlertHelper.AlertType.EMERGENCY);
        assertTrue("Texto de EMERGENCY debe mencionar 140.970",
            text.contains("140.970"));
    }
}
