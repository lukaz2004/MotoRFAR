/*
MotoRFAR — TX frequency whitelist (Resolución 5/2015)
License: GNU GPL v3
*/

package ar.motorfar.app.radio;

/**
 * Hard-limit de transmisión: solo permite TX en las 3 frecuencias VHF
 * de la Resolución 5/2015 de la Secretaría de Comunicaciones.
 *
 * Resolución 5/2015 — M.T.T.T. (uso libre sin licencia):
 *   139.970 MHz  canal prioritario  ("Grupo")
 *   138.510 MHz  canal secundario   ("Alternativo")
 *   140.970 MHz  EXCLUSIVO emerg.   ("Emergencia")
 *
 * RX no está restringida — escuchar es libre en todo el rango del SA818-V.
 * Solo TX está sujeta a esta whitelist.
 */
public class TxWhitelist {

    /** Frecuencias autorizadas para TX, en MHz (Res. 5/2015). */
    public static final float[] ALLOWED_TX_FREQS_MHZ = {
        139.970f,   // Grupo      — canal prioritario
        138.510f,   // Alternativo — canal secundario
        140.970f    // Emergencia  — uso exclusivo emergencias
    };

    /**
     * Tolerancia en MHz para comparación de flotantes.
     * 0.0005 MHz = 500 Hz — amplia para diferencias de float pero
     * estricta para canales reales (mínima separación es 1.460 MHz).
     */
    private static final float TOLERANCE_MHZ = 0.0005f;

    /**
     * Determina si se permite transmitir en la frecuencia dada.
     *
     * @param freqMhz Frecuencia en MHz.
     * @return true si la frecuencia está en la whitelist de Res. 5/2015.
     */
    public boolean canTransmit(float freqMhz) {
        for (float allowed : ALLOWED_TX_FREQS_MHZ) {
            if (Math.abs(freqMhz - allowed) <= TOLERANCE_MHZ) {
                return true;
            }
        }
        return false;
    }

    /**
     * Devuelve la frecuencia por defecto para TX (canal Grupo).
     */
    public float getDefaultTxFreq() {
        return ALLOWED_TX_FREQS_MHZ[0]; // 139.970 MHz
    }

    /**
     * Devuelve la frecuencia de emergencia (canal Emergencia).
     */
    public float getEmergencyFreq() {
        return ALLOWED_TX_FREQS_MHZ[2]; // 140.970 MHz
    }

    /**
     * 2026-07-06: la Res. 5/2015 reserva 140.970 MHz de uso EXCLUSIVO para
     * emergencias reales. Tráfico de datos de rutina (balizado APRS, chat,
     * STOP/REAGRUPAMIENTO) no debería transmitirse ahí solo porque el usuario
     * dejó el radio sintonizado en ese canal — únicamente la alerta real de
     * emergencia debe usarlo.
     */
    public boolean isEmergencyFreq(float freqMhz) {
        return Math.abs(freqMhz - getEmergencyFreq()) <= TOLERANCE_MHZ;
    }
}
