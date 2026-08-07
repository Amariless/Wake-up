package com.fritangui.wakeup.ui.clock.stopwatch

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun StopwatchScreen(viewModel: StopwatchViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            formatElapsed(state.elapsedMillis),
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.padding(top = 48.dp),
        )
        Row(modifier = Modifier.padding(top = 24.dp)) {
            OutlinedButton(onClick = { if (state.isRunning) viewModel.lap() else viewModel.reset() }, modifier = Modifier.padding(end = 8.dp)) {
                Text(if (state.isRunning) "Vuelta" else "Reiniciar")
            }
            Button(onClick = { if (state.isRunning) viewModel.pause() else viewModel.start() }) {
                Text(if (state.isRunning) "Pausar" else if (state.elapsedMillis > 0) "Reanudar" else "Iniciar")
            }
        }
        if (state.laps.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
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
