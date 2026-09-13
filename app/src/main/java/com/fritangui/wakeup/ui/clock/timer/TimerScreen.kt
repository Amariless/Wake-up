@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.animation.ExperimentalAnimationApi::class)

package com.fritangui.wakeup.ui.clock.timer

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.HourglassEmpty
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedIconButton
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
import com.fritangui.wakeup.ui.components.glowBackground

private val CHALLENGE_LABELS = mapOf(
    DismissChallengeType.NONE to "Ninguno",
    DismissChallengeType.SHAKE to "Agitar el celular",
    DismissChallengeType.MATH_PROBLEM to "Resolver una cuenta",
    DismissChallengeType.DRAW_GESTURE to "Conectar puntos",
    DismissChallengeType.TRACE_PATH to "Seguir línea curva",
    DismissChallengeType.TYPE_PHRASE to "Escribir una frase",
)

/** Minutos de los 3 atajos rápidos (#161, copiados del mockup de referencia: "00:10:00",
 *  "00:15:00", "00:30:00" como pastillas arriba del botón de Iniciar). */
private val TIMER_PRESETS_MINUTES = listOf(10, 15, 30)

private enum class TimerPhase { IDLE, RUNNING, RINGING }

@Composable
fun TimerScreen(viewModel: TimerViewModel = hiltViewModel()) {
    val state by viewModel.timerState.collectAsState()
    val challengePref by viewModel.challengePref.collectAsState()

    var hoursInput by remember { mutableIntStateOf(0) }
    var minutesInput by remember { mutableIntStateOf(5) }
    var secondsInput by remember { mutableIntStateOf(0) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }
    // Qué ruedas están mid-gesto ahora mismo (arrastrando o todavía asentándose tras soltar) —
    // mientras cualquiera lo esté, "Iniciar" queda deshabilitado (#161): antes, cambiar una rueda y
    // tocar Iniciar de inmediato podía arrancar con el valor VIEJO, porque WheelPicker recién
    // reporta el valor nuevo cuando el scroll se asienta (evita spamear onValueChange en cada
    // píxel), y hoursInput/minutesInput/secondsInput todavía no se habían actualizado a tiempo.
    var settlingWheels by remember { mutableStateOf(emptySet<String>()) }

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
        // Resplandor radial + verticalScroll de respaldo: sin el scroll, en pantallas más chicas o
        // con letra grande del sistema, las ruedas + selector de reto + botón de Iniciar podían no
        // entrar completos en alto, y el botón (lo último en el Column) quedaba cortado fuera de la
        // pantalla en vez de solo apretado.
        //
        // De vuelta a Arrangement.Center (#161): con Arrangement.Top se sacaba el corte del botón,
        // pero quedaba pegado arriba del todo, incómodo — con las ruedas ahora bastante más chicas
        // (52dp×3 en vez de 64dp×5, ver más abajo) todo el contenido junto ya entra cómodo centrado
        // en pantallas normales, sin volver a tapar el botón; verticalScroll sigue de respaldo para
        // el caso de letra grande del sistema o pantallas chicas.
        modifier = Modifier.fillMaxSize().glowBackground().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
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
                    onSelectPreset = { totalMinutes -> hoursInput = 0; minutesInput = totalMinutes; secondsInput = 0 },
                    onStart = {
                        val totalMillis = (hoursInput * 3600L + minutesInput * 60L + secondsInput) * 1000L
                        if (totalMillis > 0) viewModel.start(totalMillis, challengePref.type, challengePref.difficulty)
                    },
                    onWheelsSettlingChange = { key, settling -> settlingWheels = if (settling) settlingWheels + key else settlingWheels - key },
                    allWheelsSettled = settlingWheels.isEmpty(),
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
                        tint = MaterialTheme.colorScheme.primary,
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
    onSelectPreset: (totalMinutes: Int) -> Unit,
    onStart: () -> Unit,
    onWheelsSettlingChange: (key: String, settling: Boolean) -> Unit,
    allWheelsSettled: Boolean,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Sin tarjeta/fondo detrás (#161, mockup de referencia): las ruedas flotan directo sobre
        // el resplandor de la pantalla, como el reloj nativo — antes vivían dentro de una tarjeta
        // surfaceContainer que el usuario pidió sacar ("la caja... está muy fea").
        //
        // itemHeight/visibleCount más chicos que antes (52dp×3 = 156dp, antes 64dp×5 = 320dp): el
        // botón de Iniciar seguía quedando tapado en pantallas más chicas (#161) — las ruedas eran,
        // de lejos, lo que más alto ocupaba.
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            // Sin etiquetas "h"/"min"/"seg" arriba de cada rueda: el orden ya deja claro cuál es
            // cuál. Y con loop = true, cada rueda da la vuelta indefinidamente en cualquier
            // dirección (arriba de la hora 0 aparece la 23, arriba del segundo 0 el 59...) en vez
            // de topar con un final. Sin los ":" entre ruedas (el espaciado ya deja claro que son
            // 3 valores separados, ver #151).
            WheelPicker(
                value = hoursInput,
                range = 0..23,
                onValueChange = onHoursChange,
                loop = true,
                itemHeight = 52.dp,
                visibleCount = 3,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
                contentDescriptionLabel = "Horas",
                onSettling = { settling -> onWheelsSettlingChange("h", settling) },
            )
            WheelPicker(
                value = minutesInput,
                range = 0..59,
                onValueChange = onMinutesChange,
                loop = true,
                itemHeight = 52.dp,
                visibleCount = 3,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
                contentDescriptionLabel = "Minutos",
                onSettling = { settling -> onWheelsSettlingChange("m", settling) },
            )
            WheelPicker(
                value = secondsInput,
                range = 0..59,
                onValueChange = onSecondsChange,
                loop = true,
                itemHeight = 52.dp,
                visibleCount = 3,
                width = 96.dp,
                textStyle = MaterialTheme.typography.displaySmall,
                centerEmphasis = 1.1f,
                contentDescriptionLabel = "Segundos",
                onSettling = { settling -> onWheelsSettlingChange("s", settling) },
            )
        }

        // Atajos rápidos (#161): tocar uno pone las ruedas directo en esa duración, sin tener que
        // deslizarlas a mano para los valores más comunes.
        Row(modifier = Modifier.padding(top = 12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TIMER_PRESETS_MINUTES.forEach { totalMinutes ->
                val selected = hoursInput == 0 && minutesInput == totalMinutes && secondsInput == 0
                TimerPresetChip(
                    label = "00:%02d:00".format(totalMinutes),
                    selected = selected,
                    onClick = { onSelectPreset(totalMinutes) },
                )
            }
        }

        ChallengeSelector(
            challengeLabel = challengeLabel,
            expanded = challengeMenuExpanded,
            onExpandedChange = onChallengeMenuExpandedChange,
            onSelected = onChallengeSelected,
            modifier = Modifier.padding(top = 14.dp),
        )

        // Botón circular relleno con acento índigo (antes salvia) para calzar con el resplandor
        // púrpura del mockup de referencia — mismo acento que usa "Enfoque" en Inicio. Deshabilitado
        // mientras alguna rueda todavía se está asentando (ver el comentario de settlingWheels más
        // arriba) — evita arrancar con un valor que ya cambiaste en pantalla pero que WheelPicker
        // todavía no terminó de reportar.
        GlowingStartButton(
            onClick = onStart,
            enabled = allWheelsSettled && (hoursInput > 0 || minutesInput > 0 || secondsInput > 0),
        )
    }
}

/** Selector de reto compacto: una pastilla con el nombre del reto elegido + una flechita, en vez
 *  del ExposedDropdownMenuBox/OutlinedTextField de Material (#161: "cambia la caja... a algo más
 *  compacto y que se adapte al tema" — el campo con label flotante y borde se veía fuera de lugar
 *  sobre el resplandor de fondo). El reto elegido ya se guardaba entre sesiones (DataStore, ver
 *  TimerViewModel.setChallengePref) — eso no cambió, solo cómo se elige. */
@Composable
private fun ChallengeSelector(
    challengeLabel: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onSelected: (DismissChallengeType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .clickable { onExpandedChange(!expanded) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Reto: $challengeLabel", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
            Icon(
                Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 2.dp).size(20.dp),
            )
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { onExpandedChange(false) }) {
            CHALLENGE_LABELS.forEach { (type, text) ->
                DropdownMenuItem(text = { Text(text) }, onClick = { onSelected(type) })
            }
        }
    }
}

/** Pastilla de atajo de duración: sin relleno + borde índigo cuando está seleccionada, superficie
 *  plana cuando no — mismo lenguaje que el mockup de referencia (no el FilterChip con check de
 *  Material, que se ve distinto). */
@Composable
private fun TimerPresetChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceContainer)
            .then(
                if (selected) {
                    Modifier.border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(999.dp))
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
}

/** Botón circular con un halo propio detrás (círculo más grande, mismo color, muy tenue) — el
 *  "glow" del botón de Iniciar/Play del mockup de referencia, además del resplandor de fondo de
 *  toda la pantalla. */
@Composable
private fun GlowingStartButton(onClick: () -> Unit, enabled: Boolean) {
    Box(modifier = Modifier.padding(top = 20.dp).size(112.dp), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .size(112.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
        )
        FilledIconButton(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.size(72.dp),
            shape = CircleShape,
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
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
                color = MaterialTheme.colorScheme.primary,
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
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
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
