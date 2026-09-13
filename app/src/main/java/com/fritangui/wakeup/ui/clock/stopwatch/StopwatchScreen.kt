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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
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
 * antes: la aguja índigo (la "principal") da una vuelta por minuto y lleva el tiempo TOTAL; una
 * segunda aguja fina, del mismo largo, aparece recién con la primera vuelta y mide el tiempo desde
 * esa vuelta — se reinicia sola cada vez que se toca "Vuelta", así que siempre muestra cuánto lleva
 * el tramo actual. Debajo, la tabla de vueltas con sus 3 columnas (vuelta / parcial / total).
 */
@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val hasStarted = state.isRunning || state.elapsedMillis > 0
    // Tiempo desde la última vuelta (o desde el arranque si todavía no hay ninguna) — lo que marca
    // la aguja fina secundaria. Se recalcula solo, sin ningún estado de "reinicio" aparte: como se
    // resta contra el último valor de state.laps, en cuanto se agrega una vuelta nueva este valor
    // vuelve a ser chico solo, dando la sensación de "arranca de nuevo" sin animación especial.
    val timeSinceLastLap = state.elapsedMillis - (state.laps.lastOrNull() ?: 0L)

    Column(
        // verticalScroll en toda la pantalla (no solo dentro de la tabla de vueltas): antes la
        // tabla vivía en su propio LazyColumn sin alto acotado, así que le tocaba el espacio que
        // sobraba nomás — casi nada, ya que carátula+botones ya ocupaban casi toda la pantalla — y
        // ADEMÁS scrolleaba por su cuenta ahí adentro, mostrando una vuelta a la vez. Con todo en
        // un solo Column con scroll (y la tabla como Column normal, no Lazy) se ve y se scrollea
        // como una sola pantalla continua.
        // Arrangement.Center (#161, "centra el cronómetro también" — mismo criterio que el
        // Temporizador): sin vueltas todavía, centra la carátula+botones en la pantalla en vez de
        // dejarlos pegados arriba; en cuanto hay vueltas, el contenido crece y el scroll de respaldo
        // hace el resto.
        modifier = Modifier.fillMaxSize().glowBackground().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnalogStopwatchFace(
            elapsedMillis = state.elapsedMillis,
            timeSinceLastLap = timeSinceLastLap,
            showLapHand = state.laps.isNotEmpty(),
            modifier = Modifier.size(240.dp),
        )

        Row(
            modifier = Modifier.padding(top = 24.dp),
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
            Column(modifier = Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 16.dp)) {
                LapTableRow(
                    "Vuelta",
                    "Tiempos parciales",
                    "Tiempo total",
                    color = MaterialTheme.colorScheme.outline,
                    fontWeight = FontWeight.Normal,
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))
                // Column normal, no LazyColumn: la cantidad de vueltas de un cronómetro manual es
                // chica de sobra (nadie toca "Vuelta" cientos de veces), así que no hace falta
                // virtualización — y evita el problema de mezclar dos scrolls anidados de arriba.
                state.laps.reversed().forEachIndexed { displayIndex, lapTotalMillis ->
                    // indexOf(lapMillis) numeraba mal si dos vueltas caían en el mismo milisegundo
                    // (el tick del cronómetro es cada 31ms, así que dos toques rápidos de "Vuelta"
                    // alcanzan a caer en el mismo valor): indexOf siempre devuelve la PRIMERA
                    // coincidencia. La posición en la lista ya da el número correcto sin ese riesgo.
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

/**
 * @param timeSinceLastLap tiempo transcurrido desde la última vuelta (o desde el arranque si
 * todavía no hay ninguna) — lo que marca la aguja fina secundaria.
 * @param showLapHand la aguja secundaria no se dibuja hasta la primera vuelta (#161: "debe ser
 * invisible al principio, pero al darle a vuelta, se pone otra vez a contar desde el inicio").
 */
@Composable
private fun AnalogStopwatchFace(elapsedMillis: Long, timeSinceLastLap: Long, showLapHand: Boolean, modifier: Modifier = Modifier) {
    val tickColor = MaterialTheme.colorScheme.outline
    // La aguja PRINCIPAL es la índigo — antes era al revés (#161: "la morada debe ser la
    // principal y ser más ancha"). Marca el tiempo TOTAL, una vuelta por minuto.
    val mainHandColor = MaterialTheme.colorScheme.primary
    // La secundaria (fina, del mismo largo que la principal) marca el tiempo desde la última vuelta.
    val lapHandColor = MaterialTheme.colorScheme.onSurface
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val radius = size.minDimension / 2f
            val center = Offset(size.width / 2f, size.height / 2f)
            val handLength = radius * 0.8f

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

            // Aguja secundaria (fina): tiempo desde la última vuelta, mismo largo que la principal
            // pero mucho más delgada — y solo si ya hubo al menos una vuelta.
            if (showLapHand) {
                val lapAngleRad = ((timeSinceLastLap % 60_000L) / 60_000f * 360f - 90f) * (PI / 180f).toFloat()
                val lapDirection = Offset(cos(lapAngleRad), sin(lapAngleRad))
                drawLine(
                    color = lapHandColor,
                    start = center,
                    end = center + lapDirection * handLength,
                    strokeWidth = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            // Aguja principal (índigo, más ancha): tiempo TOTAL transcurrido, una vuelta por minuto.
            val mainAngleRad = ((elapsedMillis % 60_000L) / 60_000f * 360f - 90f) * (PI / 180f).toFloat()
            val mainDirection = Offset(cos(mainAngleRad), sin(mainAngleRad))
            drawLine(
                color = mainHandColor,
                start = center,
                end = center + mainDirection * handLength,
                strokeWidth = 4.dp.toPx(),
                cap = StrokeCap.Round,
            )
            // Círculo pequeño sobre la aguja principal, un poco separado del centro (no pegado del
            // todo) — el detalle de contrapeso que tiene el segundero de un cronógrafo real (#161,
            // ver imagen de referencia).
            drawCircle(color = mainHandColor, radius = 3.dp.toPx(), center = center + mainDirection * (handLength * 0.22f))

            drawCircle(color = mainHandColor, radius = 5.dp.toPx(), center = center)
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
