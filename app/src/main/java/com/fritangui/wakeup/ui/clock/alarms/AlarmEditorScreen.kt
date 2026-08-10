@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock.alarms

import android.media.RingtoneManager
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSounds
import com.fritangui.wakeup.alarm.sound.NotificationSounds
import com.fritangui.wakeup.alarm.ui.DismissChallengeContent
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import com.fritangui.wakeup.ui.components.AppTimePickerDialog

// "M" para miércoles (no "X"): se sobreentiende por la posición entre martes y jueves.
private val DIA_NOMBRES = listOf("L", "M", "M", "J", "V", "S", "D")

private val CHALLENGE_LABELS = mapOf(
    DismissChallengeType.NONE to "Ninguno (botón normal)",
    DismissChallengeType.SHAKE to "Agitar el celular",
    DismissChallengeType.MATH_PROBLEM to "Resolver una cuenta",
    DismissChallengeType.DRAW_GESTURE to "Conectar puntos en orden",
    DismissChallengeType.TRACE_PATH to "Seguir una línea curva",
    DismissChallengeType.TYPE_PHRASE to "Escribir una frase",
)

private val DIFFICULTY_LABELS = listOf(1 to "Fácil", 2 to "Medio", 3 to "Difícil")

@Composable
fun AlarmEditorScreen(
    onBack: () -> Unit,
    viewModel: AlarmEditorViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val alarm by viewModel.alarm.collectAsState()
    val savedFeedback by viewModel.savedFeedback.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(savedFeedback) {
        savedFeedback?.let { snackbarHostState.showSnackbar(it) }
    }

    var label by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.label ?: "") }
    var hour by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.minute ?: 0) }
    var repeatBitmask by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.repeatDaysBitmask ?: 0) }
    var kind by remember(alarm?.id) { mutableStateOf(alarm?.kind ?: AlarmKind.ALARM) }
    var challenge by remember(alarm?.id) { mutableStateOf(alarm?.dismissChallenge ?: DismissChallengeType.NONE) }
    var difficulty by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.challengeDifficulty ?: 1) }
    var vibrate by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.vibrate ?: true) }
    var soundUri by rememberSaveable(alarm?.id, kind) { mutableStateOf(alarm?.soundUri) }
    var deleteAfterRing by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.deleteAfterRing ?: false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }
    var showChallengePreview by remember { mutableStateOf(false) }

    val isReminder = kind == AlarmKind.REMINDER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Nueva alarma" else "Editar alarma") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar alarma")
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            // "Alarma" suena a pantalla completa con reto para apagarla; "Recordatorio" es solo una
            // notificación normal con su propio sonido, para avisos que no necesitan despertar a nadie.
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = kind == AlarmKind.ALARM,
                    onClick = { kind = AlarmKind.ALARM },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Alarma") }
                SegmentedButton(
                    selected = kind == AlarmKind.REMINDER,
                    onClick = { kind = AlarmKind.REMINDER },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Recordatorio") }
            }

            TextButton(onClick = { showTimePicker = true }, modifier = Modifier.padding(top = 8.dp)) {
                Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.displayLarge)
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text(if (isReminder) "Nombre del recordatorio" else "Nombre de la alarma") },
                // Placeholder, no un valor real: se ve en gris cuando el campo está vacío, pero no
                // hay nada que borrar para escribir tu propio nombre. Si lo dejas así, "Despertar"
                // es lo que de verdad se guarda (ver AlarmEditorViewModel.save).
                placeholder = { Text("Despertar") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text("Repetir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            // Sin cajas ni chips (igual que en el selector de días del horario de clase): así el
            // texto queda siempre centrado en su espacio, sin depender del padding interno de
            // FilterChip que lo desalineaba.
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                DIA_NOMBRES.forEachIndexed { index, dayLabel ->
                    val bit = AlarmEntity.dayBit(index + 1)
                    val selected = (repeatBitmask and bit) != 0
                    Box(
                        modifier = Modifier.weight(1f).clickable {
                            repeatBitmask = if (selected) repeatBitmask and bit.inv() else repeatBitmask or bit
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            dayLabel,
                            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
                            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Text("Sonido", style = MaterialTheme.typography.titleMedium)
            Card(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                        Text(
                            if (isReminder) NotificationSounds.labelFor(context, soundUri) else AlarmSounds.labelFor(context, soundUri),
                        )
                    }
                    TextButton(onClick = { showSoundPicker = true }) { Text("Cambiar") }
                }
            }

            if (!isReminder) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                Text("Reto para apagarla", style = MaterialTheme.typography.titleMedium)
                ExposedDropdownMenuBox(
                    expanded = challengeMenuExpanded,
                    onExpandedChange = { challengeMenuExpanded = it },
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    OutlinedTextField(
                        value = CHALLENGE_LABELS.getValue(challenge),
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = challengeMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                    )
                    ExposedDropdownMenu(expanded = challengeMenuExpanded, onDismissRequest = { challengeMenuExpanded = false }) {
                        CHALLENGE_LABELS.forEach { (type, text) ->
                            DropdownMenuItem(text = { Text(text) }, onClick = { challenge = type; challengeMenuExpanded = false })
                        }
                    }
                }

                if (challenge != DismissChallengeType.NONE) {
                    Text("Dificultad", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DIFFICULTY_LABELS.forEach { (value, text) ->
                            FilterChip(
                                selected = difficulty == value,
                                onClick = { difficulty = value },
                                label = { Text(text) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                    TextButton(onClick = { showChallengePreview = true }, modifier = Modifier.padding(top = 4.dp)) {
                        Text("Probar reto")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Vibrar", style = MaterialTheme.typography.bodyLarge)
                    Switch(checked = vibrate, onCheckedChange = { vibrate = it })
                }

                Text(
                    "Te avisaremos 1 hora antes con una notificación silenciosa (puedes apagar solo esa vez desde ahí).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            // Su propio divisor SIEMPRE (antes solo aparecía cuando era Alarma): en Recordatorio se
            // saltaba todo el bloque de arriba y este interruptor quedaba pegado justo debajo de la
            // tarjeta de Sonido, sin nada de aire entre los dos.
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            // Distinto de "Vibrar": esto borra la alarma/recordatorio de la lista después de que
            // suene, en vez de solo activar/desactivar la vibración.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Eliminar después de sonar", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Útil para algo de una sola vez: no se vuelve a guardar tras sonar.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                    )
                }
                Switch(checked = deleteAfterRing, onCheckedChange = { deleteAfterRing = it })
            }

            Button(
                onClick = {
                    viewModel.save(
                        label = label,
                        hour = hour,
                        minute = minute,
                        repeatDaysBitmask = repeatBitmask,
                        kind = kind,
                        challenge = challenge,
                        difficulty = difficulty,
                        vibrate = vibrate,
                        soundUri = soundUri,
                        preAlarmMinutesBefore = 60,
                        deleteAfterRing = deleteAfterRing,
                        onSaved = onBack,
                    )
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text("Guardar") }
        }
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialHour = hour,
            initialMinute = minute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m -> hour = h; minute = m; showTimePicker = false },
        )
    }

    if (showSoundPicker) {
        if (isReminder) {
            AlarmSoundPickerDialog(
                currentUri = soundUri,
                bundledSounds = NotificationSounds.BUNDLED.map { it.label to NotificationSounds.uriFor(context, it.rawResId) },
                title = "Sonido del recordatorio",
                ringtoneType = RingtoneManager.TYPE_NOTIFICATION,
                onDismiss = { showSoundPicker = false },
                onSelect = { soundUri = it; showSoundPicker = false },
            )
        } else {
            AlarmSoundPickerDialog(
                currentUri = soundUri,
                bundledSounds = AlarmSounds.BUNDLED.map { it.label to AlarmSounds.uriFor(context, it.rawResId) },
                title = "Sonido de la alarma",
                ringtoneType = RingtoneManager.TYPE_ALARM,
                onDismiss = { showSoundPicker = false },
                onSelect = { soundUri = it; showSoundPicker = false },
            )
        }
    }

    if (showChallengePreview) {
        Dialog(onDismissRequest = { showChallengePreview = false }) {
            Card {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                    Text("Probando: ${CHALLENGE_LABELS.getValue(challenge)}", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "Esto es solo una prueba, no apaga ninguna alarma real.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp, bottom = 16.dp),
                    )
                    DismissChallengeContent(
                        type = challenge,
                        difficulty = difficulty,
                        onCompleted = { showChallengePreview = false },
                    )
                    TextButton(
                        onClick = { showChallengePreview = false },
                        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    ) { Text("Cerrar") }
                }
            }
        }
    }
}
