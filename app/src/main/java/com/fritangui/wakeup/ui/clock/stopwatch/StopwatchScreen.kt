package com.fritangui.wakeup.ui.clock.stopwatch

import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Un solo botón grande de "Iniciar" mientras está en cero (como el cronómetro nativo), que al
 * arrancar se convierte en dos botones circulares: uno secundario (vuelta/reiniciar, según el
 * estado) y uno principal relleno (pausar/reanudar) — en vez de dos `OutlinedButton`/`Button` de
 * texto plano puestos uno al lado del otro.
 *
 * Pase visual (#161): el tiempo transcurrido y las vueltas ahora viven dentro de tarjetas
 * `surfaceContainer` redondeadas, en vez de texto suelto sobre el fondo — mismo lenguaje que ya
 * usan Inicio y el resto de pantallas del rediseño, con el acento "salvia" (tertiary) que el
 * mockup reserva para Reloj.
 */
@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val hasStarted = state.isRunning || state.elapsedMillis > 0

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        // Mismo tamaño de anillo (220dp) que el círculo de progreso del temporizador, para que
        // ambas pantallas del reloj se sientan como la misma familia visual.
        Box(
            modifier = Modifier
                .padding(top = 40.dp)
                .size(220.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                formatElapsed(state.elapsedMillis),
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
            )
        }

        Row(
            modifier = Modifier.padding(top = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            if (hasStarted) {
                OutlinedIconButton(
                    onClick = { if (state.isRunning) viewModel.lap() else viewModel.reset() },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                ) {
                    Icon(
                        if (state.isRunning) Icons.Default.Flag else Icons.Default.Replay,
                        contentDescription = if (state.isRunning) "Vuelta" else "Reiniciar",
                    )
                }
            }
            FilledIconButton(
                onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() },
                modifier = Modifier.size(72.dp),
                shape = CircleShape,
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isRunning) "Pausar" else if (state.elapsedMillis > 0) "Reanudar" else "Iniciar",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (state.laps.isNotEmpty()) {
            Text(
                "Vueltas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth().padding(top = 32.dp, bottom = 10.dp),
            )
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // indexOf(lapMillis) numeraba mal si dos vueltas caían en el mismo milisegundo (el
                // tick del cronómetro es cada 31ms, así que dos toques rápidos de "Vuelta" alcanzan
                // a caer en el mismo valor): indexOf siempre devuelve la PRIMERA coincidencia, así
                // que la vuelta repetida se numeraba igual que la original en vez de con su propio
                // número. La posición en la lista ya da el número correcto sin ese riesgo.
                itemsIndexed(state.laps.reversed()) { displayIndex, lapMillis ->
                    val lapNumber = state.laps.size - displayIndex
                    val isFirst = displayIndex == 0
                    val isLast = displayIndex == state.laps.lastIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(
                                RoundedCornerShape(
                                    topStart = if (isFirst) 16.dp else 0.dp,
                                    topEnd = if (isFirst) 16.dp else 0.dp,
                                    bottomStart = if (isLast) 16.dp else 0.dp,
                                    bottomEnd = if (isLast) 16.dp else 0.dp,
                                ),
                            )
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Vuelta $lapNumber", modifier = Modifier.weight(1f))
                        Text(formatElapsed(lapMillis), fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

private fun formatElapsed(millis: Long): String {
    val totalCentis = millis / 10
    val minutes = totalCentis / 6000
    val seconds = (totalCentis / 100) % 60
    val centis = totalCentis % 100
    return "%02d:%02d.%02d".format(minutes, seconds, centis)
}
