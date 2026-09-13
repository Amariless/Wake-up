package com.fritangui.wakeup.ui.clock.stopwatch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.ui.components.glowBackground
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Carátula analógica (#161, mockup de referencia) en vez del cronómetro puramente digital de
 * antes: aguja fina que da una vuelta por minuto (como el segundero de un cronógrafo real) y una
 * más corta y gruesa, en índigo, que da una vuelta por hora — con la lectura digital superpuesta
 * en el centro. Debajo, la tabla de vueltas con sus 3 columnas (vuelta / parcial / total) del
 * mismo mockup, en vez de una sola columna con el tiempo acumulado.
 */
@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val hasStarted = state.isRunning || state.elapsedMillis > 0

    Column(
        modifier = Modifier.fillMaxSize().glowBackground().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnalogStopwatchFace(
            elapsedMillis = state.elapsedMillis,
            modifier = Modifier.padding(top = 24.dp).size(240.dp),
        )

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            if (hasStarted) {
                LargeRoundTextButton(
                    text = if (state.isRunning) "Vuelta" else "Reiniciar",
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    contentColor = MaterialTheme.colorScheme.onSurface,
                    onClick = { if (state.isRunning) viewModel.lap() else viewModel.reset() },
                )
            }
            LargeRoundTextButton(
                text = if (state.isRunning) "Pausar" else if (state.elapsedMillis > 0) "Reanudar" else "Iniciar",
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() },
            )
        }

        if (state.laps.isNotEmpty()) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp)) {
                LapTableRow(
                    "Vuelta",
                    "Tiempos parciales",
                    "Tiempo total",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Normal,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    // indexOf(lapMillis) numeraba mal si dos vueltas caían en el mismo milisegundo
                    // (el tick del cronómetro es cada 31ms, así que dos toques rápidos de "Vuelta"
                    // alcanzan a caer en el mismo valor): indexOf siempre devuelve la PRIMERA
                    // coincidencia. La posición en la lista ya da el número correcto sin ese riesgo.
                    itemsIndexed(state.laps.reversed()) { displayIndex, lapTotalMillis ->
                        val lapIndex = state.laps.size - 1 - displayIndex
                        val lapNumber = lapIndex + 1
                        val splitMillis = lapTotalMillis - (state.laps.getOrNull(lapIndex - 1) ?: 0L)
                        LapTableRow(
                            "%02d".format(lapNumber),
                            formatElapsed(splitMillis),
                            formatElapsed(lapTotalMillis),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Medium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LapTableRow(lap: String, split: String, total: String, color: Color, fontWeight: FontWeight) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Text(lap, color = color, fontWeight = fontWeight, modifier = Modifier.weight(0.7f))
        Text(split, color = color, fontWeight = fontWeight, modifier = Modifier.weight(1.3f))
        Text(total, color = color, fontWeight = fontWeight, modifier = Modifier.weight(1f), textAlign = TextAlign.End)
    }
}

@Composable
private fun LargeRoundTextButton(text: String, containerColor: Color, contentColor: Color, onClick: () -> Unit) {
    Box(
        modifier = Modifier.size(80.dp).clip(CircleShape).background(containerColor).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge, color = contentColor, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
private fun AnalogStopwatchFace(elapsedMillis: Long, modifier: Modifier = Modifier) {
    val tickColor = MaterialTheme.colorScheme.outline
    val secondHandColor = MaterialTheme.colorScheme.onSurface
    val minuteHandColor = MaterialTheme.colorScheme.primary
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)

            drawCircle(color = tickColor.copy(alpha = 0.18f), radius = radius, center = center, style = Stroke(width = 1.dp.toPx()))

            // 60 marcas, una más larga/marcada cada 5 (como los minutos de un reloj real).
            for (tick in 0 until 60) {
                val isMajor = tick % 5 == 0
                val angleRad = (tick * 6f - 90f) * (PI / 180f).toFloat()
                val outer = radius - 2.dp.toPx()
                val inner = outer - (if (isMajor) 10.dp.toPx() else 4.dp.toPx())
                val direction = Offset(cos(angleRad), sin(angleRad))
                drawLine(
                    color = tickColor.copy(alpha = if (isMajor) 0.55f else 0.25f),
                    start = center + direction * inner,
                    end = center + direction * outer,
                    strokeWidth = if (isMajor) 2.dp.toPx() else 1.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Segundero fino: una vuelta completa por minuto (como el cronógrafo de un reloj real).
            val secondsAngleRad = ((elapsedMillis % 60_000L) / 60_000f * 360f - 90f) * (PI / 180f).toFloat()
            drawLine(
                color = secondHandColor,
                start = center,
                end = center + Offset(cos(secondsAngleRad), sin(secondsAngleRad)) * (radius * 0.8f),
                strokeWidth = 2.dp.toPx(),
                cap = StrokeCap.Round,
            )

            // Aguja corta en índigo: una vuelta completa por hora — se mueve mucho más lento, da
            // una referencia de "cuánto llevamos" de un vistazo sin tener que leer el número.
            val minutesAngleRad = ((elapsedMillis % 3_600_000L) / 3_600_000f * 360f - 90f) * (PI / 180f).toFloat()
            drawLine(
                color = minuteHandColor,
                start = center,
                end = center + Offset(cos(minutesAngleRad), sin(minutesAngleRad)) * (radius * 0.5f),
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )

            drawCircle(color = minuteHandColor, radius = 5.dp.toPx(), center = center)
        }
        Text(formatElapsed(elapsedMillis), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
    }
}

private fun formatElapsed(millis: Long): String {
    val totalCentis = millis / 10
    val minutes = totalCentis / 6000
    val seconds = (totalCentis / 100) % 60
    val centis = totalCentis % 100
    return "%02d:%02d,%02d".format(minutes, seconds, centis)
}
