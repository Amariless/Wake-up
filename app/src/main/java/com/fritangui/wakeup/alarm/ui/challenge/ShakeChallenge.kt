package com.fritangui.wakeup.alarm.ui.challenge

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.getSystemService
import kotlin.math.sqrt

/**
 * Reto "agitar el celular": obliga a un movimiento físico difícil de hacer
 * dormido sin darse cuenta. Cuenta sacudidas fuertes (aceleración por encima
 * de un umbral) hasta llegar a [requiredShakes]; escala con [difficulty].
 */
@Composable
fun ShakeChallenge(difficulty: Int, onCompleted: () -> Unit) {
    val context = LocalContext.current
    val requiredShakes = remember(difficulty) { (6 + difficulty * 4).coerceAtMost(30) }
    var shakeCount by remember { mutableIntStateOf(0) }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService<SensorManager>()
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        var lastShakeAtMs = 0L

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val (x, y, z) = event.values
                val gForce = sqrt((x * x + y * y + z * z).toDouble()) / SensorManager.GRAVITY_EARTH
                val now = System.currentTimeMillis()
                if (gForce > SHAKE_THRESHOLD_G && now - lastShakeAtMs > SHAKE_DEBOUNCE_MS) {
                    lastShakeAtMs = now
                    shakeCount++
                    if (shakeCount >= requiredShakes) onCompleted()
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        if (accelerometer != null) {
            sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        }
        onDispose { sensorManager?.unregisterListener(listener) }
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Agita el celular con fuerza para apagar la alarma")
        Text("$shakeCount / $requiredShakes")
        LinearProgressIndicator(
            progress = { (shakeCount.toFloat() / requiredShakes).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

private const val SHAKE_THRESHOLD_G = 2.2
private const val SHAKE_DEBOUNCE_MS = 250L
