package com.fritangui.wakeup.ui.clock.stopwatch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Un solo botón grande de "Iniciar" mientras está en cero (como el cronómetro nativo), que al
 * arrancar se convierte en dos botones circulares: uno secundario (vuelta/reiniciar, según el
 * estado) y uno principal relleno (pausar/reanudar) — en vez de dos `OutlinedButton`/`Button` de
 * texto plano puestos uno al lado del otro.
 */
@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val hasStarted = state.isRunning || state.elapsedMillis > 0

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatElapsed(state.elapsedMillis),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 64.dp),
        )

        Row(
            modifier = Modifier.padding(top = 40.dp),
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
                colors = IconButtonDefaults.filledIconButtonColors(),
            ) {
                Icon(
                    if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (state.isRunning) "Pausar" else if (state.elapsedMillis > 0) "Reanudar" else "Iniciar",
                    modifier = Modifier.size(32.dp),
                )
            }
        }

        if (state.laps.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(state.laps.reversed()) { lapMillis ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text("Vuelta ${state.laps.indexOf(lapMillis) + 1}", modifier = Modifier.weight(1f))
                        Text(formatElapsed(lapMillis))
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
