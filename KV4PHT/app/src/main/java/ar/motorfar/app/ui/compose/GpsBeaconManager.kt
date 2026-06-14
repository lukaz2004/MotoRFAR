package ar.motorfar.app.ui.compose

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Datos de movimiento que alimentan el balizado.
 * speedKmh y headingDeg vienen del GPS; si no hay fix, speed=0 y heading=null.
 */
data class MovementState(
    val speedKmh: Double = 0.0,
    val headingDeg: Double? = null
)

/**
 * Gestiona el balizado periódico de posición.
 *
 * Soporta dos modos:
 *  - SmartBeaconing (smartEnabled = true): intervalo adaptativo por velocidad/rumbo
 *  - Manual (smartEnabled = false): intervalo fijo de [intervalFlow]
 *
 * [movementProvider] devuelve la velocidad y rumbo actuales (del GPS).
 */
class GpsBeaconManager(
    private val onSendBeacon: () -> Unit,
    private val intervalFlow: Flow<Long>,
    private val smartEnabledProvider: () -> Boolean = { false },
    private val movementProvider: () -> MovementState = { MovementState() }
) {
    private var job: Job? = null
    private val smartBeaconing = SmartBeaconing()

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            intervalFlow.collectLatest { intervalMs ->
                smartBeaconing.reset()
                if (smartEnabledProvider()) {
                    runSmartLoop()
                } else {
                    runFixedLoop(intervalMs)
                }
            }
        }
    }

    /** Modo fijo: baliza cada intervalMs sin importar la velocidad. */
    private suspend fun runFixedLoop(intervalMs: Long) {
        while (currentCoroutineContext().isActive) {
            if (!smartEnabledProvider()) {
                onSendBeacon()
                delay(intervalMs)
            } else {
                // El usuario activó SmartBeaconing en caliente → cambiar de modo
                runSmartLoop()
                return
            }
        }
    }

    /**
     * Modo SmartBeaconing: chequea cada segundo si corresponde balizar,
     * según velocidad y cambios de rumbo. El primer beacon sale enseguida.
     */
    private suspend fun runSmartLoop() {
        var firstDone = false
        while (currentCoroutineContext().isActive) {
            if (!smartEnabledProvider()) return  // volvió a modo manual

            val mv = movementProvider()
            val now = System.currentTimeMillis()

            if (!firstDone) {
                onSendBeacon()
                // Inicializa el estado interno del algoritmo
                smartBeaconing.shouldBeacon(now, mv.speedKmh, mv.headingDeg)
                firstDone = true
            } else if (smartBeaconing.shouldBeacon(now, mv.speedKmh, mv.headingDeg)) {
                onSendBeacon()
            }

            delay(1000L)  // resolución de 1s para capturar curvas a tiempo
        }
    }

    fun stop() {
        job?.cancel()
        job = null
    }
}
