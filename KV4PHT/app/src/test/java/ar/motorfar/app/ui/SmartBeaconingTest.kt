package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.SmartBeaconing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifica el algoritmo SmartBeaconing: intervalo adaptativo por velocidad
 * y disparo por cambios de rumbo (corner pegging).
 */
class SmartBeaconingTest {

    private fun newSB() = SmartBeaconing(
        lowSpeedKmh = 5.0,
        highSpeedKmh = 100.0,
        slowRateSec = 300,
        fastRateSec = 30,
        turnMinDeg = 28.0,
        turnTimeSec = 15,
        turnSlopeDeg = 26.0
    )

    @Test
    fun rate_at_standstill_is_slow() {
        val sb = newSB()
        assertEquals(300, sb.beaconRateSec(0.0))
    }

    @Test
    fun rate_at_high_speed_is_fast() {
        val sb = newSB()
        assertEquals(30, sb.beaconRateSec(120.0))
    }

    @Test
    fun rate_at_highway_speed_is_short() {
        val sb = newSB()
        // A 100 km/h debe estar en el mínimo (30s)
        assertEquals(30, sb.beaconRateSec(100.0))
    }

    @Test
    fun rate_interpolates_at_mid_speed() {
        val sb = newSB()
        // A 50 km/h, entre lento y rápido — debe estar entre 30 y 300
        val rate = sb.beaconRateSec(50.0)
        assertTrue("Rate $rate debe estar entre 30 y 300", rate in 31..299)
    }

    @Test
    fun rate_decreases_with_speed() {
        val sb = newSB()
        val slow = sb.beaconRateSec(20.0)
        val fast = sb.beaconRateSec(80.0)
        assertTrue("A mayor velocidad, menor intervalo", fast < slow)
    }

    @Test
    fun first_beacon_after_interval_fires() {
        val sb = newSB()
        // Primer chequeo inicializa; tras superar el intervalo a alta velocidad, dispara
        val t0 = 0L
        sb.shouldBeacon(t0, 100.0, 0.0)  // inicializa
        // 31 segundos después (> 30s de fastRate)
        assertTrue(sb.shouldBeacon(t0 + 31_000L, 100.0, 0.0))
    }

    @Test
    fun no_beacon_before_interval() {
        val sb = newSB()
        val t0 = 0L
        sb.shouldBeacon(t0, 100.0, 0.0)  // inicializa + dispara
        // Solo 5 segundos después — no debe disparar (fastRate es 30s)
        assertFalse(sb.shouldBeacon(t0 + 5_000L, 100.0, 0.0))
    }

    @Test
    fun sharp_turn_triggers_beacon() {
        val sb = newSB()
        val t0 = 0L
        sb.shouldBeacon(t0, 80.0, 0.0)  // rumbo inicial 0°, a 80 km/h
        // 16s después (> turnTime 15s), giro de 90° → debe disparar por corner pegging
        assertTrue(sb.shouldBeacon(t0 + 16_000L, 80.0, 90.0))
    }

    @Test
    fun small_turn_does_not_trigger() {
        val sb = newSB()
        val t0 = 0L
        sb.shouldBeacon(t0, 80.0, 0.0)
        // Giro pequeño de 5° a los 16s — no alcanza el umbral
        assertFalse(sb.shouldBeacon(t0 + 16_000L, 80.0, 5.0))
    }

    @Test
    fun turn_ignored_when_stationary() {
        val sb = newSB()
        val t0 = 0L
        sb.shouldBeacon(t0, 0.0, 0.0)
        // Parado, aunque el rumbo cambie (deriva de brújula), no debe disparar por giro
        assertFalse(sb.shouldBeacon(t0 + 16_000L, 2.0, 180.0))
    }

    @Test
    fun reset_clears_state() {
        val sb = newSB()
        sb.shouldBeacon(0L, 100.0, 0.0)
        sb.reset()
        // Tras reset, el primer chequeo vuelve a comportarse como inicial
        // (lastBeaconAtMs = 0, así que con tiempo grande dispara)
        assertTrue(sb.shouldBeacon(400_000L, 100.0, 0.0))
    }
}
