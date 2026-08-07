package com.fritangui.wakeup.alarm.ui.challenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Reto de "seguir la línea": una curva senoidal con varias ondulaciones cruza
 * la pantalla y el usuario debe recorrerla completa con el dedo, de inicio a
 * fin, sin despegarse más de una banda de tolerancia ni soltar el dedo. Exige
 * atención sostenida y coordinación fina, más difícil de hacer dormido que un
 * simple botón.
 */
@Composable
fun TraceCurvyPathChallenge(difficulty: Int, onCompleted: () -> Unit) {
    val waves = (2 + difficulty).coerceIn(2, 6)
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var offTrack by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Recorre la línea completa con el dedo, sin salirte ni soltar")
        if (offTrack) Text("Te saliste de la línea, vuelve a intentar", color = MaterialTheme.colorScheme.error)
        LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth())

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val tolerancePx = 60f
            val samples = remember(widthPx, heightPx, waves) { buildCurvyPathSamples(widthPx, heightPx, waves) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(waves, widthPx, heightPx) {
                        if (samples.isEmpty()) return@pointerInput
                        var progressIndex = 0
                        detectDragGestures(
                            onDragStart = { start ->
                                progressIndex = if (distanceTo(start, samples[0]) < tolerancePx) 0 else -1
                                progressFraction = 0f
                                offTrack = false
                            },
                            onDrag = { change, _ ->
                                if (progressIndex < 0) return@detectDragGestures
                                // Avanza mientras el punto quede cerca de alguno de los siguientes puntos de la curva.
                                var advanced = progressIndex
                                val lookahead = (samples.size * 0.08f).toInt().coerceAtLeast(4)
                                for (i in progressIndex until minOf(progressIndex + lookahead, samples.size)) {
                                    if (distanceTo(change.position, samples[i]) < tolerancePx) advanced = i
                                }
                                if (distanceTo(change.position, samples[advanced]) > tolerancePx) {
                                    offTrack = true
                                    progressIndex = -1
                                    progressFraction = 0f
                                    return@detectDragGestures
                                }
                                progressIndex = advanced
                                progressFraction = progressIndex.toFloat() / (samples.size - 1)
                                if (progressIndex >= samples.size - 2) onCompleted()
                            },
                            onDragEnd = {
                                if (progressIndex in 0 until samples.size - 2) {
                                    offTrack = true
                                    progressFraction = 0f
                                }
                            },
                        )
                    },
            ) {
                if (samples.size > 1) {
                    val path = Path().apply {
                        moveTo(samples[0].x, samples[0].y)
                        samples.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(path, color = Color(0xFF9AA5B1), style = Stroke(width = tolerancePx * 2, cap = androidx.compose.ui.graphics.StrokeCap.Round))
                    drawPath(path, color = Color(0xFF3D5AFE), style = Stroke(width = 6f))
                    drawCircle(Color(0xFF00BFA6), radius = 16f, center = samples.first())
                    drawCircle(Color(0xFFEF5350), radius = 16f, center = samples.last())
                }
            }
        }
    }
}

private fun distanceTo(a: Offset, b: Offset): Float = hypot(a.x - b.x, a.y - b.y)

private fun buildCurvyPathSamples(widthPx: Float, heightPx: Float, waves: Int, sampleCount: Int = 120): List<Offset> {
    if (widthPx <= 0f || heightPx <= 0f) return emptyList()
    val margin = 70f
    val amplitude = (heightPx / 2f - margin).coerceAtLeast(20f)
    val midY = heightPx / 2f
    return List(sampleCount) { i ->
        val fraction = i / (sampleCount - 1).toFloat()
        val x = margin + fraction * (widthPx - 2 * margin)
        val y = midY + amplitude * sin(fraction * waves * 2 * PI.toFloat())
        Offset(x, y)
    }
}
