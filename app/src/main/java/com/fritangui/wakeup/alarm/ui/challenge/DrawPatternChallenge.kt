package com.fritangui.wakeup.alarm.ui.challenge

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.hypot
import kotlin.random.Random

/**
 * Reto de "gesto específico": el usuario debe conectar N puntos numerados en
 * orden ascendente con un solo trazo continuo (como un patrón de desbloqueo).
 * Requiere coordinación motriz consciente, mucho más difícil de hacer dormido
 * que tocar un botón grande de "apagar".
 */
@Composable
fun DrawPatternChallenge(difficulty: Int, onCompleted: () -> Unit) {
    val dotCount = (3 + difficulty).coerceIn(3, 6)
    var nextExpected by remember(dotCount) { mutableStateOf(0) }
    var failedFlash by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Traza los puntos en orden, del 1 al $dotCount, sin soltar el dedo")
        if (failedFlash) {
            Text("Orden incorrecto, vuelve a intentar", color = MaterialTheme.colorScheme.error)
        }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth().height(280.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()
            val dots = remember(dotCount, widthPx, heightPx) {
                generateDotPositions(dotCount, widthPx, heightPx)
            }

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .pointerInput(dotCount) {
                        detectDragGestures(
                            onDragStart = { start ->
                                nextExpected = if (nearestDotIndex(start, dots) == 0) 1 else 0
                            },
                            onDrag = { change, _ ->
                                val hit = nearestDotIndex(change.position, dots)
                                if (hit == nextExpected) {
                                    nextExpected++
                                    if (nextExpected >= dotCount) onCompleted()
                                } else if (hit != null && hit >= nextExpected) {
                                    failedFlash = true
                                    nextExpected = 0
                                }
                            },
                            onDragEnd = {
                                if (nextExpected in 1 until dotCount) {
                                    failedFlash = true
                                    nextExpected = 0
                                }
                            },
                        )
                    },
            ) {
                dots.forEachIndexed { index, offset ->
                    val done = index < nextExpected
                    drawCircle(
                        color = if (done) Color(0xFF00BFA6) else Color(0xFF9AA5B1),
                        radius = 26f,
                        center = offset,
                        style = Stroke(width = if (done) 8f else 4f),
                    )
                }
                if (nextExpected > 1) {
                    for (i in 1 until nextExpected) {
                        drawLine(
                            color = Color(0xFF00BFA6),
                            start = dots[i - 1],
                            end = dots[i],
                            strokeWidth = 6f,
                        )
                    }
                }
            }
        }
    }
}

private fun nearestDotIndex(position: Offset, dots: List<Offset>, hitRadiusPx: Float = 70f): Int? {
    var closestIndex: Int? = null
    var closestDistance = Float.MAX_VALUE
    dots.forEachIndexed { index, dot ->
        val d = hypot(position.x - dot.x, position.y - dot.y)
        if (d < hitRadiusPx && d < closestDistance) {
            closestDistance = d
            closestIndex = index
        }
    }
    return closestIndex
}

private fun generateDotPositions(count: Int, widthPx: Float, heightPx: Float): List<Offset> {
    if (widthPx <= 0f || heightPx <= 0f) return emptyList()
    val margin = 60f
    val random = Random(count * 7919 + widthPx.toInt())
    return List(count) {
        Offset(
            x = margin + random.nextFloat() * (widthPx - 2 * margin).coerceAtLeast(1f),
            y = margin + random.nextFloat() * (heightPx - 2 * margin).coerceAtLeast(1f),
        )
    }
}
