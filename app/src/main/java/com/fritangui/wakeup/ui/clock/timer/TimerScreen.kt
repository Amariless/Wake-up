package com.fritangui.wakeup.ui.clock.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.DismissChallengeType

private val CHALLENGE_LABELS = mapOf(
    DismissChallengeType.NONE to "Ninguno",
    DismissChallengeType.SHAKE to "Agitar el celular",
    DismissChallengeType.MATH_PROBLEM to "Resolver una cuenta",
    DismissChallengeType.DRAW_GESTURE to "Conectar puntos",
    DismissChallengeType.TRACE_PATH to "Seguir línea curva",
    DismissChallengeType.TYPE_PHRASE to "Escribir una frase",
)

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.timerState.collectAsState()
    val challengePref by viewModel.challengePref.collectAsState()

    var minutesInput by remember { mutableIntStateOf(5) }
    var secondsInput by remember { mutableIntStateOf(0) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }

    val isIdle = state.totalMillis == 0L && !state.isRinging

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (isIdle) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                NumberStepper(value = minutesInput, suffix = "min", onChange = { minutesInput = it.coerceIn(0, 180) })
                Text(" : ", style = MaterialTheme.typography.headlineMedium)
                NumberStepper(value = secondsInput, suffix = "s", onChange = { secondsInput = it.coerceIn(0, 59) })
            }

            ExposedDropdownMenuBox(
                expanded = challengeMenuExpanded,
                onExpandedChange = { challengeMenuExpanded = it },
                modifier = Modifier.padding(top = 24.dp),
            ) {
                OutlinedTextField(
                    value = CHALLENGE_LABELS.getValue(challengePref.type),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Reto para apagarlo") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = challengeMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(expanded = challengeMenuExpanded, onDismissRequest = { challengeMenuExpanded = false }) {
                    CHALLENGE_LABELS.forEach { (type, text) ->
                        DropdownMenuItem(
                            text = { Text(text) },
                            onClick = {
                                viewModel.setChallengePref(type, challengePref.difficulty)
                                challengeMenuExpanded = false
                            },
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val totalMillis = (minutesInput * 60L + secondsInput) * 1000L
                    if (totalMillis > 0) viewModel.start(totalMillis, challengePref.type, challengePref.difficulty)
                },
                enabled = minutesInput > 0 || secondsInput > 0,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Iniciar") }
        } else if (!state.isRinging) {
            val progress = if (state.totalMillis > 0) state.remainingMillis.toFloat() / state.totalMillis else 0f
            TimerProgressIndicator(progress)
            Text(formatMillis(state.remainingMillis), style = MaterialTheme.typography.displayLarge)
            Row(modifier = Modifier.padding(top = 24.dp)) {
                if (state.isRunning) {
                    OutlinedButton(onClick = viewModel::pause, modifier = Modifier.padding(end = 8.dp)) { Text("Pausar") }
                } else {
                    OutlinedButton(onClick = viewModel::resume, modifier = Modifier.padding(end = 8.dp)) { Text("Reanudar") }
                }
                Button(onClick = viewModel::cancel) { Text("Cancelar") }
            }
        } else {
            Text("¡Sonando! Ábrelo desde la notificación", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun TimerProgressIndicator(progress: Float) {
    CircularProgressIndicator(progress = { progress }, modifier = Modifier.padding(bottom = 16.dp))
}

@Composable
private fun NumberStepper(value: Int, suffix: String, onChange: (Int) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedButton(onClick = { onChange(value + if (suffix == "min") 1 else 5) }) { Text("+") }
        Text("$value $suffix", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(vertical = 4.dp))
        OutlinedButton(onClick = { onChange(value - if (suffix == "min") 1 else 5) }) { Text("−") }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
