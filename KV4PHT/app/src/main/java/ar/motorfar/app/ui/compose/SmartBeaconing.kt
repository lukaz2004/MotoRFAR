package ar.motorfar.app.ui.compose

/**
 * SmartBeaconing™ — algoritmo adaptativo de balizado APRS.
 *
 * Ajusta el intervalo entre beacons según la velocidad y los cambios de rumbo,
 * en lugar de un intervalo fijo. Beneficios:
 *
 *  - Parado / lento  → beacons espaciados (ahorra batería y no satura el canal)
 *  - En ruta rápido  → beacons frecuentes (posición precisa en el mapa)
 *  - En una curva    → beacon inmediato (captura el cambio de dirección)
 *
 * Basado en el algoritmo original de Tony Arnerich KD7TA / Steve Bragg KA9MVA.
 *
 * Parámetros por defecto pensados para moto en ruta argentina:
 *  - lowSpeed   = 5 km/h   → por debajo, usa slowRate
 *  - highSpeed  = 100 km/h → por encima, usa fastRate
 *  - slowRate   = 300 s    → beacon cada 5 min cuando vas lento/parado
 *  - fastRate   = 30 s     → beacon cada 30 s a velocidad de ruta
 *  - turnMin    = 28°      → giro mínimo para disparar un beacon por rumbo
 *  - turnTime   = 15 s     → tiempo mínimo entre beacons por giro (anti-spam)
 */
class SmartBeaconing(
    private val lowSpeedKmh: Double = 5.0,
    private val highSpeedKmh: Double = 100.0,
    private val slowRateSec: Int = 300,
    private val fastRateSec: Int = 30,
    private val turnMinDeg: Double = 28.0,
    private val turnTimeSec: Int = 15,
    private val turnSlopeDeg: Double = 26.0
) {
    private var lastBeaconAtMs: Long = 0L
    private var lastHeadingDeg: Double = -1.0

    /**
     * Calcula el intervalo (en segundos) entre beacons para una velocidad dada.
     * Interpola linealmente entre slowRate y fastRate según la velocidad.
     */
    fun beaconRateSec(speedKmh: Double): Int {
        return when {
            speedKmh <= lowSpeedKmh  -> slowRateSec
            speedKmh >= highSpeedKmh -> fastRateSec
            else -> {
                // Interpolación: a más velocidad, menor intervalo
                val ratio = (speedKmh - lowSpeedKmh) / (highSpeedKmh - lowSpeedKmh)
                (slowRateSec - (ratio * (slowRateSec - fastRateSec))).toInt()
            }
        }
    }

    /**
     * Decide si corresponde emitir un beacon AHORA.
     *
     * @param nowMs       timestamp actual
     * @param speedKmh    velocidad actual (km/h)
     * @param headingDeg  rumbo actual (0-360°), o null si no disponible
     * @return true si se debe balizar en este instante
     */
    fun shouldBeacon(nowMs: Long, speedKmh: Double, headingDeg: Double?): Boolean {
        val elapsedSec = (nowMs - lastBeaconAtMs) / 1000.0
        val rate = beaconRateSec(speedKmh)

        // Registra el rumbo de referencia la primera vez que se conoce
        if (headingDeg != null && lastHeadingDeg < 0) {
            lastHeadingDeg = headingDeg
        }

        // 1. Disparo por intervalo de tiempo (según velocidad)
        if (elapsedSec >= rate) {
            commit(nowMs, headingDeg)
            return true
        }

        // 2. Disparo por cambio de rumbo (corner pegging) — solo si estás en movimiento
        if (headingDeg != null && lastHeadingDeg >= 0 && speedKmh > lowSpeedKmh) {
            var turn = kotlin.math.abs(headingDeg - lastHeadingDeg)
            if (turn > 180) turn = 360 - turn  // normaliza el giro al lado más corto

            // Umbral de giro: más exigente a baja velocidad (turnSlope/speed)
            val threshold = turnMinDeg + turnSlopeDeg / speedKmh.coerceAtLeast(1.0) * 10.0
            if (turn >= threshold && elapsedSec >= turnTimeSec) {
                commit(nowMs, headingDeg)
                return true
            }
        }

        return false
    }

    private fun commit(nowMs: Long, headingDeg: Double?) {
        lastBeaconAtMs = nowMs
        if (headingDeg != null) lastHeadingDeg = headingDeg
    }

    fun reset() {
        lastBeaconAtMs = 0L
        lastHeadingDeg = -1.0
    }
}
