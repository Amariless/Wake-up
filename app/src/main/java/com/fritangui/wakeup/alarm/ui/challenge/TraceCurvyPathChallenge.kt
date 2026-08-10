package com.fritangui.wakeup.alarm.ui.challenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Reto de "seguir la línea": una curva senoidal con varias ondulaciones cruza
 * la pantalla y el usuario debe recorrerla completa con el dedo, de inicio a
 * fin, sin soltar. La banda de tolerancia es generosa a propósito (más ancha
 * que el grosor visible del trazo) para que no se sienta injusto: exige
 * atención sostenida, no precisión milimétrica.
 */
@Composable
fun TraceCurvyPathChallenge(difficulty: Int, onCompleted: () -> Unit) {
    val waves = (2 + difficulty).coerceIn(2, 6)
    // Más generoso que antes, y un poco más angosto en dificultades altas — pero nunca por debajo de 90px.
    val tolerancePx = (130f - difficulty * 15f).coerceAtLeast(90f)
    var progressFraction by remember { mutableFloatStateOf(0f) }
    var offTrack by remember { mutableStateOf(false) }

    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val startColor = MaterialTheme.colorScheme.tertiary
    val endColor = MaterialTheme.colorScheme.error
    val cursorColor = MaterialTheme.colorScheme.secondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Recorre la línea completa con el dedo, sin soltar")
        // Alto fijo reservado para el aviso: antes el texto solo aparecía cuando offTrack era true,
        // así que todo lo de abajo (la barra de progreso, el propio trazo) se movía de golpe cada
        // vez que aparecía o desaparecía — eso era el "salto a lo loco" que se veía.
        Box(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            if (offTrack) {
                Text(
                    "Un poco más cerca de la línea…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        LinearProgressIndicator(progress = { progressFraction }, modifier = Modifier.fillMaxWidth())

        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val samples = remember(widthPx, heightPx, waves) { buildCurvyPathSamples(widthPx, heightPx, waves) }
            var cursorIndex by remember(samples) { mutableFloatStateOf(0f) }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(waves, widthPx, heightPx) {
                        if (samples.isEmpty()) return@pointerInput
                        var progressIndex = 0
                        val lookahead = (samples.size * 0.15f).toInt().coerceAtLeast(6)
                        val retreatOnMiss = (samples.size * 0.06f).toInt().coerceAtLeast(2)
                        // Cuántos eventos de arrastre seguidos fuera de tolerancia hacen falta antes
                        // de considerar que "de verdad" se salió (en vez de reaccionar al primer
                        // evento, que con el dedo temblando cerca del borde de la tolerancia
                        // alternaba on/off en cada frame — el mensaje parpadeaba y el progreso
                        // retrocedía de a poquito en cada uno de esos frames, dando la sensación de
                        // saltar sin control).
                        val missThreshold = 4
                        var missStreak = 0
                        detectDragGestures(
                            onDragStart = { start ->
                                progressIndex = if (distanceTo(start, samples[0]) < tolerancePx * 1.3f) 0 else -1
                                progressFraction = 0f
                                cursorIndex = 0f
                                offTrack = false
                                missStreak = 0
                            },
                            onDrag = { change, _ ->
                                if (progressIndex < 0) return@detectDragGestures
                                var advanced = progressIndex
                                for (i in progressIndex until minOf(progressIndex + lookahead, samples.size)) {
                                    if (distanceTo(change.position, samples[i]) < tolerancePx) advanced = i
                                }
                                val onTrack = advanced > progressIndex || distanceTo(change.position, samples[progressIndex]) < tolerancePx
                                if (onTrack) {
                                    missStreak = 0
                                    offTrack = false
                                    progressIndex = advanced
                                    cursorIndex = progressIndex.toFloat()
                                    progressFraction = progressIndex.toFloat() / (samples.size - 1)
                                    if (progressIndex >= samples.size - 2) onCompleted()
                                    return@detectDragGestures
                                }
                                missStreak++
                                if (missStreak >= missThreshold) {
                                    // Retrocede una sola vez por racha de fallos, no en cada frame
                                    // mientras sigue fuera — si no, el progreso se desplomaba a cada
                                    // instante (~60 veces por segundo) mientras el dedo seguía fuera.
                                    offTrack = true
                                    progressIndex = (progressIndex - retreatOnMiss).coerceAtLeast(0)
                                    cursorIndex = progressIndex.toFloat()
                                    progressFraction = progressIndex.toFloat() / (samples.size - 1)
                                    missStreak = 0
                                }
                            },
                            onDragEnd = {
                                if (progressIndex in 0 until samples.size - 2) {
                                    // Soltar antes de tiempo también retrocede un poco en vez de perder todo.
                                    progressIndex = (progressIndex - retreatOnMiss).coerceAtLeast(0)
                                    cursorIndex = progressIndex.toFloat()
                                    progressFraction = progressIndex.toFloat() / (samples.size - 1)
                                }
                                offTrack = false
                                missStreak = 0
                            },
                        )
                    },
            ) {
                if (samples.size > 1) {
                    val path = Path().apply {
                        moveTo(samples[0].x, samples[0].y)
                        samples.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    // Banda de tolerancia (guía visual de cuánto margen hay de verdad).
                    drawPath(path, color = trackColor, style = Stroke(width = tolerancePx * 2, cap = StrokeCap.Round))
                    // Línea principal.
                    drawPath(path, color = lineColor, style = Stroke(width = 10f, cap = StrokeCap.Round))
                    drawCircle(startColor, radius = 18f, center = samples.first())
                    drawCircle(endColor, radius = 18f, center = samples.last())
                    // Cursor: dónde va el usuario ahora mismo en la curva.
                    val idx = cursorIndex.toInt().coerceIn(0, samples.size - 1)
                    drawCircle(cursorColor, radius = 22f, center = samples[idx])
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
