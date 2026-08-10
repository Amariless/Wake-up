package com.fritangui.wakeup.ui.clock.alarms

import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSounds
import com.fritangui.wakeup.alarm.sound.NotificationSounds
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.domain.AlarmTiming
import kotlinx.datetime.Clock

private val DIA_LETRAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun AlarmsListScreen(
    onOpenAlarm: (Long) -> Unit,
    viewModel: AlarmsViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsState()
    val playingUri by viewModel.previewPlayer.playingUri.collectAsState()
    val context = LocalContext.current

    // Se recalcula cada tanto (no solo al recomponer por otro motivo) para que el "faltan Xh Ym" de
    // cada fila no se quede pegado en el valor de cuando se abrió la pantalla si el usuario la deja
    // abierta un rato.
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = Clock.System.now()
        }
    }

    if (alarms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Toca + para crear tu primera alarma del reloj general")
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(alarms, key = { it.id }) { alarm ->
            val soundUri = alarm.soundUri
                ?: if (alarm.kind == AlarmKind.REMINDER) NotificationSounds.defaultSoundUriFor(context) else AlarmSounds.defaultSoundUriFor(context)
            AlarmRow(
                alarm = alarm,
                now = now,
                isPreviewing = playingUri == soundUri,
                onClick = { onOpenAlarm(alarm.id) },
                onToggle = { viewModel.setEnabled(alarm.id, it) },
                onPreview = { viewModel.previewPlayer.toggle(soundUri) },
                modifier = Modifier.animateItem(placementSpec = tween(220)),
            )
        }
    }
}

@Composable
private fun AlarmRow(
    alarm: AlarmEntity,
    now: kotlinx.datetime.Instant,
    isPreviewing: Boolean,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
                Text(
                    alarm.label.ifBlank { if (alarm.kind == AlarmKind.REMINDER) "Recordatorio" else "Alarma" },
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (alarm.kind == AlarmKind.REMINDER) {
                    Text("Recordatorio", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.secondary)
                }
                Text(repeatSummary(alarm.repeatDaysBitmask), style = MaterialTheme.typography.bodyMedium)
                val trigger = AlarmTiming.nextTrigger(alarm, now = now)
                if (trigger != null) {
                    Text(
                        "Faltan ${AlarmTiming.formatRemaining(trigger - now)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onPreview) {
                    Icon(
                        if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = "Previsualizar sonido",
                    )
                }
                Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
            }
        }
    }
}

private fun repeatSummary(bitmask: Int): String {
    if (bitmask == 0) return "Una vez"
    if (bitmask == 0b1111111) return "Todos los días"
    if (bitmask == 0b0011111) return "Lunes a viernes"
    return (1..7).filter { (bitmask and AlarmEntity.dayBit(it)) != 0 }.joinToString(" ") { DIA_LETRAS[it - 1] }
}
