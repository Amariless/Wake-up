@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock.alarms

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSounds
import com.fritangui.wakeup.alarm.sound.NotificationSounds
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.domain.AlarmTiming
import com.fritangui.wakeup.ui.components.ClockTimeText
import com.fritangui.wakeup.ui.components.glowBackground
import kotlinx.datetime.Clock

private val DIA_LETRAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun AlarmsListScreen(
    onOpenAlarm: (Long) -> Unit,
    viewModel: AlarmsViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsState()
    val scope by viewModel.scope.collectAsState()
    val activeFolder by viewModel.activeFolder.collectAsState()
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

    // Mismo resplandor de fondo que Temporizador/Cronómetro (#161: "usá la misma línea estética
    // para mejorar la UI de toda la app en general") — empieza a extender ese lenguaje visual acá.
    Column(modifier = Modifier.fillMaxSize().glowBackground()) {
        // Toggle "Generales" / carpeta principal (#161): antes esta pestaña solo mostraba las
        // alarmas del reloj general, sin ninguna forma de ver las de la carpeta activa desde acá
        // (había que entrar a esa carpeta y su propia pestaña de Alarmas).
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            SegmentedButton(
                selected = scope == AlarmsScope.GENERAL,
                onClick = { viewModel.setScope(AlarmsScope.GENERAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
            ) { Text("Generales") }
            SegmentedButton(
                selected = scope == AlarmsScope.ACTIVE_FOLDER,
                onClick = { viewModel.setScope(AlarmsScope.ACTIVE_FOLDER) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                enabled = activeFolder != null,
            ) { Text(activeFolder?.name ?: "Carpeta principal") }
        }

        if (alarms.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    when {
                        scope == AlarmsScope.ACTIVE_FOLDER && activeFolder == null ->
                            "Marcá una carpeta como principal (⭐ desde su pantalla) para ver sus alarmas acá"
                        scope == AlarmsScope.ACTIVE_FOLDER ->
                            "Toca + para crear la primera alarma de \"${activeFolder?.name}\""
                        else -> "Toca + para crear tu primera alarma del reloj general"
                    },
                    textAlign = TextAlign.Center,
                )
            }
            return@Column
        }

        // Secciones separadas (no solo una etiqueta chica) para que quede clarísimo que un
        // Recordatorio es distinto de una Alarma: es solo una notificación normal, no algo que
        // "suena" y haya que apagar completando un reto.
        val realAlarms = alarms.filter { it.kind == AlarmKind.ALARM }
        val reminders = alarms.filter { it.kind == AlarmKind.REMINDER }

        // Modifier.fillMaxSize() explícito: sin él, el LazyColumn se queda "envuelto" al tamaño de
        // su contenido en vez de ocupar todo el alto que le da el HorizontalPager de ClockScreen —
        // dentro de un Box (como arma cada página del pager) eso lo deja centrado verticalmente en
        // vez de pegado arriba, con espacio vacío arriba Y abajo (el bug reportado del "espacio vacío").
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp, 8.dp)) {
            if (realAlarms.isNotEmpty()) {
                item(key = "header_alarms") { SectionHeader("Alarmas") }
                items(realAlarms, key = { it.id }) { alarm ->
                    val soundUri = alarm.soundUri ?: AlarmSounds.defaultSoundUriFor(context)
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
            if (reminders.isNotEmpty()) {
                item(key = "header_reminders") { SectionHeader("Recordatorios", topPadding = if (realAlarms.isNotEmpty()) 20.dp else 0.dp) }
                items(reminders, key = { it.id }) { alarm ->
                    val soundUri = alarm.soundUri ?: NotificationSounds.defaultSoundUriFor(context)
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
    }
}

@Composable
private fun SectionHeader(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(
        text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = topPadding, bottom = 4.dp),
    )
}

/** Tarjeta grande (#161, mockup de referencia): la hora es lo más grande de la fila, con un
 *  divisor propio separando el repetir/cuenta regresiva del interruptor — en vez de una fila
 *  compacta de ícono+hora+switch todo en una sola línea. */
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
    val accent = if (alarm.kind == AlarmKind.REMINDER) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(MaterialTheme.shapes.large)
            // Semi-transparente (#161, mockup de referencia: "que se vea el degradado del fondo")
            // en vez de una superficie opaca — así se nota el resplandor de la pantalla por detrás,
            // y de paso la tarjeta pesa menos visualmente (antes se sentía con demasiado aire).
            .background(MaterialTheme.colorScheme.surfaceContainer.copy(alpha = 0.6f))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier.size(32.dp).clip(CircleShape).background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                // Ícono distinto (campana vs. despertador) además de la sección separada: un
                // Recordatorio es solo una notificación normal, no algo que suene y haya que apagar.
                Icon(
                    if (alarm.kind == AlarmKind.REMINDER) Icons.Default.Notifications else Icons.Default.Alarm,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            IconButton(onClick = onPreview, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isPreviewing) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = "Previsualizar sonido",
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        ClockTimeText(alarm.hour, alarm.minute, style = MaterialTheme.typography.displaySmall, modifier = Modifier.padding(top = 4.dp))
        if (alarm.label.isNotBlank()) {
            Text(alarm.label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column {
                Text(repeatSummary(alarm.repeatDaysBitmask, alarm.deleteAfterRing), fontWeight = FontWeight.Medium)
                val trigger = AlarmTiming.nextTrigger(alarm, now = now)
                if (trigger != null) {
                    Text(
                        "Faltan ${AlarmTiming.formatRemaining(trigger - now)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
            }
            Switch(
                checked = alarm.isEnabled,
                onCheckedChange = onToggle,
                // Sin esto, un usuario de TalkBack que llega a este switch en una lista con
                // varias alarmas solo escucha "interruptor, activado/desactivado", sin saber a
                // cuál de las filas corresponde.
                modifier = Modifier.semantics {
                    contentDescription = "${if (alarm.kind == AlarmKind.REMINDER) "Recordatorio" else "Alarma"} " +
                        "%02d:%02d".format(alarm.hour, alarm.minute) +
                        (alarm.label.takeIf { it.isNotBlank() }?.let { ", $it" } ?: "")
                },
            )
        }
    }
}

// Sin ningún día marcado, ahora suena todos los días salvo que se vaya a borrar tras sonar (ver
// AlarmTiming.nextTrigger) — el resumen tiene que reflejar ese mismo criterio.
private fun repeatSummary(bitmask: Int, deleteAfterRing: Boolean): String {
    if (bitmask == 0) return if (deleteAfterRing) "Una vez" else "Todos los días"
    if (bitmask == 0b1111111) return "Todos los días"
    if (bitmask == 0b0011111) return "Lunes a viernes"
    return (1..7).filter { (bitmask and AlarmEntity.dayBit(it)) != 0 }.joinToString(" ") { DIA_LETRAS[it - 1] }
}
