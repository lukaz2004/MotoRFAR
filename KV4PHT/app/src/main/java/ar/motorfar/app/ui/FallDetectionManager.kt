package ar.motorfar.app.ui

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.sqrt

/**
 * Detecta caídas/impactos usando el acelerómetro.
 * Lógica: Impacto fuerte (> 25m/s²) seguido de un periodo de quietud.
 */
class FallDetectionManager(
    context: Context,
    private val onFallDetected: (peakAcceleration: Float) -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    // 2026-07-07: TYPE_ACCELEROMETER incluye la gravedad (~9.8 m/s² en reposo),
    // así que QUIET_THRESHOLD (1.5f) nunca se cumplía con el celular quieto --
    // el Man-Down se cancelaba solo después de cada golpe. LINEAR_ACCELERATION
    // resta la gravedad (reposo real ~0), que es lo que el umbral de quietud
    // necesita para funcionar.
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var lastImpactTime = 0L
    // ponytail: 25 (2.5G) disparaba con un golpe de mesa y seguro con vibración
    // de motor de moto en marcha -- subido a ~5G, umbral real de calibración de
    // campo (probar andando en moto y ajustar según falsos positivos/negativos).
    private val IMPACT_THRESHOLD = 50f // Aprox 5G
    private val QUIET_THRESHOLD = 1.5f // Movimiento mínimo (linear accel, sin gravedad)
    private val WAIT_AFTER_IMPACT_MS = 2000L // Esperar 2s para ver si quedó quieto

    private var isMonitoringQuiet = false
    private var peakAcceleration = 0f // Pico del golpe, para escalar la cuenta regresiva

    fun start() {
        accelerometer?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        
        val acceleration = sqrt(x * x + y * y + z * z)

        if (acceleration > IMPACT_THRESHOLD) {
            lastImpactTime = System.currentTimeMillis()
            isMonitoringQuiet = true
            if (acceleration > peakAcceleration) peakAcceleration = acceleration
        } else if (isMonitoringQuiet) {
            val now = System.currentTimeMillis()
            if (now - lastImpactTime > WAIT_AFTER_IMPACT_MS) {
                // Si después del impacto la aceleración es baja (quieto)
                if (acceleration < QUIET_THRESHOLD) {
                    isMonitoringQuiet = false
                    val peak = peakAcceleration
                    peakAcceleration = 0f
                    onFallDetected(peak)
                } else {
                    // Si se mueve, cancelamos el monitoreo de caída
                    isMonitoringQuiet = false
                    peakAcceleration = 0f
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
