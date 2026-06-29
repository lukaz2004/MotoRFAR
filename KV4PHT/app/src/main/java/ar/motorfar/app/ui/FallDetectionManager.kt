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
    private val onFallDetected: () -> Unit
) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    
    private var lastImpactTime = 0L
    private val IMPACT_THRESHOLD = 25f // Aprox 2.5G
    private val QUIET_THRESHOLD = 1.5f // Movimiento mínimo
    private val WAIT_AFTER_IMPACT_MS = 2000L // Esperar 2s para ver si quedó quieto
    
    private var isMonitoringQuiet = false

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
        } else if (isMonitoringQuiet) {
            val now = System.currentTimeMillis()
            if (now - lastImpactTime > WAIT_AFTER_IMPACT_MS) {
                // Si después del impacto la aceleración es baja (quieto)
                if (acceleration < QUIET_THRESHOLD) {
                    isMonitoringQuiet = false
                    onFallDetected()
                } else {
                    // Si se mueve, cancelamos el monitoreo de caída
                    isMonitoringQuiet = false
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
