@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package com.fritangui.wakeup.ui.clock.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
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

    // Recuerda el último hh:mm:ss usado en vez de arrancar siempre en "5 min" fijo.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        val seconds = viewModel.currentLastDurationSeconds()
        hoursInput = seconds / 3600
        minutesInput = (seconds % 3600) / 60
        secondsInput = seconds % 60
    }

    val phase = when {
        state.isRinging -> TimerPhase.RINGING
        state.totalMillis > 0L -> TimerPhase.RUNNING
        else -> TimerPhase.IDLE
    }

    Column(
        // verticalScroll de respaldo: sin él, en pantallas más chicas o con letra grande del
        // sistema, la tarjeta de ruedas + selector de reto + botón de Iniciar podían no entrar
        // completos en alto, y el botón (lo último en el Column) quedaba cortado fuera de la
        // pantalla en vez de solo apretado — nada indicaba que hubiera más contenido debajo.
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
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
                TimerPhase.RINGING -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.HourglassEmpty,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(48.dp),
                    )
                    Text(
                        "¡Sonando!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        "Ábrelo desde la notificación",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
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
        // Las ruedas viven dentro de una tarjeta surfaceContainer redondeada (#161) — mismo
        // lenguaje que el resto del rediseño, en vez de flotar sueltas sobre el fondo.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            // Sin etiquetas "h"/"min"/"seg" arriba de cada rueda: el orden ya deja claro cuál es
            // cuál. Y con loop = true, cada rueda da la vuelta indefinidamente en cualquier
            // dirección (arriba de la hora 0 aparece la 23, arriba del segundo 0 el 59...) en vez
            // de topar con un final. Sin los ":" entre ruedas (el espaciado ya deja claro que son
            // 3 valores separados, ver #151) y con la rueda en general más grande: más alto por
            // ítem, más ancha, letra más grande y con un poco más de énfasis extra en el número
            // central.
            WheelPicker(
                value = hoursInput,
                range = 0..23,
                onValueChange = onHoursChange,
                loop = true,
                itemHeight = 64.dp,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
                contentDescriptionLabel = "Horas",
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
                contentDescriptionLabel = "Minutos",
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
                contentDescriptionLabel = "Segundos",
            )
        }

        ExposedDropdownMenuBox(
            expanded = challengeMenuExpanded,
            onExpandedChange = onChallengeMenuExpandedChange,
            modifier = Modifier.padding(top = 20.dp),
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

        // Mismo botón circular relleno que usa el cronómetro para "Iniciar" (mismo acento salvia
        // también), en vez de un botón de ancho completo con texto — para que ambas pantallas del
        // reloj se sientan consistentes.
        FilledIconButton(
            onClick = onStart,
            enabled = hoursInput > 0 || minutesInput > 0 || secondsInput > 0,
            modifier = Modifier.padding(top = 20.dp).size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.tertiary,
                contentColor = MaterialTheme.colorScheme.onTertiary,
            ),
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
    val haptics = LocalHapticFeedback.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val targetProgress = if (totalMillis > 0) remainingMillis.toFloat() / totalMillis else 0f
        // Ni siquiera hace falta que el tick se vea "saltar": se anima suave entre cada valor,
        // así que aunque el servicio actualice cada 500ms, visualmente es una transición continua.
        val animatedProgress by animateFloatAsState(targetValue = targetProgress, animationSpec = tween(450), label = "timer_progress")
        // Anillo grande con el tiempo adentro (en vez de un anillo chico + el número debajo): así
        // se lee de un vistazo cuánto falta sin tener que separar la mirada entre dos elementos.
        Box(modifier = Modifier.size(220.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.tertiary,
                strokeWidth = 10.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                strokeCap = StrokeCap.Round,
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatMillis(remainingMillis), style = MaterialTheme.typography.displayMedium)
                Text(
                    "RESTANTE",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
        Row(modifier = Modifier.padding(top = 32.dp), horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            OutlinedIconButton(
                onClick = { haptics.performHapticFeedback(HapticFeedbackType.LongPress); onStop() },
                modifier = Modifier.size(52.dp),
            ) {
                Icon(Icons.Default.Stop, contentDescription = "Apagar")
            }
            FilledIconButton(
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    if (isRunning) onPause() else onResume()
                },
                modifier = Modifier.size(68.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary,
                ),
            ) {
                Icon(
                    if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isRunning) "Pausar" else "Reanudar",
                    modifier = Modifier.size(28.dp),
                )
            }
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
