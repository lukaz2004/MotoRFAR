package com.vagell.kv4pht.ui.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class GpsBeaconManager(
    private val onSendBeacon: () -> Unit,
    private val intervalFlow: Flow<Long>
) {
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            intervalFlow.collectLatest { intervalMs ->
                while (isActive) {
                    onSendBeacon()
                    delay(intervalMs)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
