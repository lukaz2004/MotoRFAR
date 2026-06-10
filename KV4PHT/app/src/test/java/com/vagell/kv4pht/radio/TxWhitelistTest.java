/*
MotoRFAR — TX whitelist tests (Regulación Res. 5/2015)
Verifica que solo se permita transmitir en las 3 frecuencias VHF autorizadas.
License: GNU GPL v3
*/

package com.vagell.kv4pht.radio;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

/**
 * Verifica el hard-limit de TX: solo las 3 frecuencias de Res. 5/2015 pueden
 * transmitir. Cualquier otra — incluyendo bandas de radioaficionados, PMR446,
 * FRS/GMRS, o frecuencias arbitrarias — debe ser bloqueada por software.
 *
 * Esto es un requisito regulatorio, no una preferencia UX.
 */
public class TxWhitelistTest {

    private TxWhitelist whitelist;

    @Before
    public void setUp() {
        whitelist = new TxWhitelist();
    }

    // --- Frecuencias PERMITIDAS (Res. 5/2015) ---

    @Test
    public void grupo_139970_allowed() {
        assertTrue(whitelist.canTransmit(139.970f));
    }

    @Test
    public void alternativo_138510_allowed() {
        assertTrue(whitelist.canTransmit(138.510f));
    }

    @Test
    public void emergencia_140970_allowed() {
        assertTrue(whitelist.canTransmit(140.970f));
    }

    // Tolerancia float: valores muy cercanos siguen siendo válidos
    @Test
    public void grupo_withFloatTolerance_allowed() {
        assertTrue(whitelist.canTransmit(139.9701f));
        assertTrue(whitelist.canTransmit(139.9699f));
    }

    // --- Frecuencias BLOQUEADAS ---

    @Test
    public void aprs_144800_blocked() {
        assertFalse(whitelist.canTransmit(144.800f));
    }

    @Test
    public void ham2m_146520_simplex_blocked() {
        assertFalse(whitelist.canTransmit(146.520f));
    }

    @Test
    public void ham2m_range_blocked() {
        assertFalse(whitelist.canTransmit(144.0f));
        assertFalse(whitelist.canTransmit(148.0f));
    }

    @Test
    public void ham70cm_range_blocked() {
        assertFalse(whitelist.canTransmit(430.0f));
        assertFalse(whitelist.canTransmit(440.0f));
    }

    @Test
    public void pmr446_blocked() {
        assertFalse(whitelist.canTransmit(446.006f));
        assertFalse(whitelist.canTransmit(446.194f));
    }

    @Test
    public void frsGmrs_blocked() {
        assertFalse(whitelist.canTransmit(462.5625f));
        assertFalse(whitelist.canTransmit(462.7125f));
    }

    @Test
    public void nearMttt_butNotExact_blocked() {
        // 139.000 no es ninguno de los 3 canales
        assertFalse(whitelist.canTransmit(139.000f));
        // 138.500 no es 138.510
        assertFalse(whitelist.canTransmit(138.500f));
        // 141.000 no es 140.970
        assertFalse(whitelist.canTransmit(141.000f));
    }

    @Test
    public void zero_blocked() {
        assertFalse(whitelist.canTransmit(0.0f));
    }
}
