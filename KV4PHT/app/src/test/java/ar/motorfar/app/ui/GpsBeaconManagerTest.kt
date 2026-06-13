package ar.motorfar.app.ui

import ar.motorfar.app.ui.compose.GpsBeaconManager
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GpsBeaconManagerTest {

    @Test
    fun beacon_called_once_immediately_on_start() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)
        advanceTimeBy(1L)
        manager.stop()
        assertEquals(1, callCount)
    }

    @Test
    fun beacon_called_again_after_interval() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)
        advanceTimeBy(60_001L)
        manager.stop()
        assertEquals(2, callCount)
    }

    @Test
    fun stop_prevents_further_calls() = runTest {
        var callCount = 0
        val intervalFlow = MutableStateFlow(60_000L)
        val manager = GpsBeaconManager(onSendBeacon = { callCount++ }, intervalFlow = intervalFlow)
        manager.start(this)
        advanceTimeBy(1L)
        manager.stop()
        advanceTimeBy(300_000L)
        assertEquals(1, callCount)
    }
}
