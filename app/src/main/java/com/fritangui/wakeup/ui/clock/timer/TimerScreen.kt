@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package com.fritangui.wakeup.ui.clock.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
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
import com.fritangui.wakeup.ui.components.WheelPicker

private val CHALLENGE_LABELS = mapOf(
    DismissChallengeType.NONE to "Ninguno",
    DismissChallengeType.SHAKE to "Agitar el celular",
    DismissChallengeType.MATH_PROBLEM to "Resolver una cuenta",
    DismissChallengeType.DRAW_GESTURE to "Conectar puntos",
    DismissChallengeType.TRACE_PATH to "Seguir línea curva",
    DismissChallengeType.TYPE_PHRASE to "Escribir una frase",
)

private enum class TimerPhase { IDLE, RUNNING, RINGING }

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.timerState.collectAsState()
    val challengePref by viewModel.challengePref.collectAsState()

    var hoursInput by remember { mutableIntStateOf(0) }
    var minutesInput by remember { mutableIntStateOf(5) }
    var secondsInput by remember { mutableIntStateOf(0) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }

    val phase = when {
        state.isRinging -> TimerPhase.RINGING
        state.totalMillis > 0L -> TimerPhase.RUNNING
        else -> TimerPhase.IDLE
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AnimatedContent(
            targetState = phase,
            transitionSpec = { (fadeIn(tween(220)) togetherWith fadeOut(tween(160))) },
            label = "timer_phase",
        ) { currentPhase ->
            when (currentPhase) {
                TimerPhase.IDLE -> TimerIdleContent(
                    hoursInput = hoursInput,
                    minutesInput = minutesInput,
                    secondsInput = secondsInput,
                    onHoursChange = { hoursInput = it },
                    onMinutesChange = { minutesInput = it },
                    onSecondsChange = { secondsInput = it },
                    challengeLabel = CHALLENGE_LABELS.getValue(challengePref.type),
                    challengeMenuExpanded = challengeMenuExpanded,
                    onChallengeMenuExpandedChange = { challengeMenuExpanded = it },
                    onChallengeSelected = { type -> viewModel.setChallengePref(type, challengePref.difficulty); challengeMenuExpanded = false },
                    onStart = {
                        val totalMillis = (hoursInput * 3600L + minutesInput * 60L + secondsInput) * 1000L
                        if (totalMillis > 0) viewModel.start(totalMillis, challengePref.type, challengePref.difficulty)
                    },
                )
                TimerPhase.RUNNING -> TimerRunningContent(
                    remainingMillis = state.remainingMillis,
                    totalMillis = state.totalMillis,
                    isRunning = state.isRunning,
                    onPause = viewModel::pause,
                    onResume = viewModel::resume,
                    onStop = viewModel::cancel,
                )
                TimerPhase.RINGING -> Text("¡Sonando! Ábrelo desde la notificación", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun TimerIdleContent(
    hoursInput: Int,
    minutesInput: Int,
    secondsInput: Int,
    onHoursChange: (Int) -> Unit,
    onMinutesChange: (Int) -> Unit,
    onSecondsChange: (Int) -> Unit,
    challengeLabel: String,
    challengeMenuExpanded: Boolean,
    onChallengeMenuExpandedChange: (Boolean) -> Unit,
    onChallengeSelected: (DismissChallengeType) -> Unit,
    onStart: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Sin etiquetas "h"/"min"/"seg" arriba de cada rueda: el orden ya deja claro cuál es cuál.
        // Y con loop = true, cada rueda da la vuelta indefinidamente en cualquier dirección (arriba
        // de la hora 0 aparece la 23, arriba del segundo 0 el 59...) en vez de topar con un final.
        // Sin los ":" entre ruedas (el espaciado ya deja claro que son 3 valores separados, ver
        // #151) y con la rueda en general más grande: más alto por ítem, más ancha, letra más
        // grande y con un poco más de énfasis extra en el número central.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            WheelPicker(
                value = hoursInput,
                range = 0..23,
                onValueChange = onHoursChange,
                loop = true,
                itemHeight = 64.dp,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
            )
            WheelPicker(
                value = minutesInput,
                range = 0..59,
                onValueChange = onMinutesChange,
                loop = true,
                itemHeight = 64.dp,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
            )
            WheelPicker(
                value = secondsInput,
                range = 0..59,
                onValueChange = onSecondsChange,
                loop = true,
                itemHeight = 64.dp,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
            )
        }

        ExposedDropdownMenuBox(
            expanded = challengeMenuExpanded,
            onExpandedChange = onChallengeMenuExpandedChange,
            modifier = Modifier.padding(top = 24.dp),
        ) {
            OutlinedTextField(
                value = challengeLabel,
                onValueChange = {},
                readOnly = true,
                label = { Text("Reto para apagarlo") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = challengeMenuExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor(),
            )
            ExposedDropdownMenu(expanded = challengeMenuExpanded, onDismissRequest = { onChallengeMenuExpandedChange(false) }) {
                CHALLENGE_LABELS.forEach { (type, text) ->
                    DropdownMenuItem(text = { Text(text) }, onClick = { onChallengeSelected(type) })
                }
            }
        }

        // Mismo botón circular relleno que usa el cronómetro para "Iniciar", en vez de un botón de
        // ancho completo con texto — para que ambas pantallas del reloj se sientan consistentes.
        FilledIconButton(
            onClick = onStart,
            enabled = hoursInput > 0 || minutesInput > 0 || secondsInput > 0,
            modifier = Modifier.padding(top = 24.dp).size(72.dp),
            shape = CircleShape,
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar", modifier = Modifier.size(32.dp))
        }
    }
}

@Composable
private fun TimerRunningContent(
    remainingMillis: Long,
    totalMillis: Long,
    isRunning: Boolean,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val targetProgress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
        // Ni siquiera hace falta que el tick se vea "saltar": se anima suave entre cada valor,
        // así que aunque el servicio actualice cada 500ms, visualmente es una transición continua.
        val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(450), label = "timer_progress")
        CircularProgressIndicator(progress = { animatedProgress }, modifier = Modifier.padding(bottom = 16.dp))
        Text(formatMillis(remainingMillis), style = MaterialTheme.typography.displayLarge)
        Row(modifier = Modifier.padding(top = 24.dp)) {
            if (isRunning) {
                OutlinedButton(onClick = onPause, modifier = Modifier.padding(end = 8.dp)) { Text("Pausar") }
            } else {
                OutlinedButton(onClick = onResume, modifier = Modifier.padding(end = 8.dp)) { Text("Reanudar") }
            }
            Button(onClick = onStop) { Text("Apagar") }
        }
    }
}

private fun formatMillis(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
