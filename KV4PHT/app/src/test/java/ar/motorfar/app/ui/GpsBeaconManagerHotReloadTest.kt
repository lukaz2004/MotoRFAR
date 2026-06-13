package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.GpsBeaconManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GpsBeaconManagerHotReloadTest {

    @Test
    fun interval_change_fires_immediately_then_uses_new_interval() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)

        advanceTimeBy(1L)          // first beacon at t=0
        assertEquals(1, callCount)

        intervalFlow.value = 30_000L
        advanceTimeBy(1L)          // collectLatest cancels delay, restarts, fires immediately
        assertEquals(2, callCount)

        advanceTimeBy(30_001L)     // fires again at new 30s interval
        assertEquals(3, callCount)

        manager.stop()
    }

    @Test
    fun interval_change_does_not_fire_at_old_interval() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)

        advanceTimeBy(1L)          // first beacon
        assertEquals(1, callCount)

        intervalFlow.value = 30_000L
        advanceTimeBy(1L)          // restart beacon
        assertEquals(2, callCount)

        // 59s — would have fired on old 60s interval but NOT on 30s (already fired at 30s)
        advanceTimeBy(58_000L)     // t = 60s — 30s interval fires at t=30, t=60 → 2 more
        // Actually: after restart at t≈2ms, waits 30s → fires at t≈30002ms,
        //           waits another 30s → fires at t≈60002ms — not within this advanceTimeBy
        assertEquals(3, callCount)

        manager.stop()
    }

    @Test
    fun multiple_interval_changes_always_use_latest_value() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)

        advanceTimeBy(1L)
        assertEquals(1, callCount)

        intervalFlow.value = 120_000L
        advanceTimeBy(1L)          // restarts with 120s, fires immediately
        assertEquals(2, callCount)

        intervalFlow.value = 30_000L
        advanceTimeBy(1L)          // restarts with 30s, fires immediately
        assertEquals(3, callCount)

        advanceTimeBy(30_001L)     // fires at 30s
        assertEquals(4, callCount)

        manager.stop()
    }
}
