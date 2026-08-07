@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock.alarms

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSounds
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import com.fritangui.wakeup.ui.components.AppTimePickerDialog

private val DIA_NOMBRES = listOf("L", "M", "X", "J", "V", "S", "D")

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

    var label by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.label ?: "") }
    var hour by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.hour ?: 7) }
    var minute by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.minute ?: 0) }
    var repeatBitmask by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.repeatDaysBitmask ?: 0) }
    var challenge by remember(alarm?.id) { mutableStateOf(alarm?.dismissChallenge ?: DismissChallengeType.NONE) }
    var difficulty by rememberSaveable(alarm?.id) { mutableIntStateOf(alarm?.challengeDifficulty ?: 1) }
    var vibrate by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.vibrate ?: true) }
    var soundUri by rememberSaveable(alarm?.id) { mutableStateOf(alarm?.soundUri) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showSoundPicker by remember { mutableStateOf(false) }
    var challengeMenuExpanded by remember { mutableStateOf(false) }

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
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            TextButton(onClick = { showTimePicker = true }) {
                Text("%02d:%02d".format(hour, minute), style = MaterialTheme.typography.displayLarge)
            }

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Nombre de la alarma") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text("Repetir", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                DIA_NOMBRES.forEachIndexed { index, dayLabel ->
                    val bit = AlarmEntity.dayBit(index + 1)
                    val selected = (repeatBitmask and bit) != 0
                    FilterChip(
                        selected = selected,
                        onClick = { repeatBitmask = if (selected) repeatBitmask and bit.inv() else repeatBitmask or bit },
                        label = { Text(dayLabel) },
                        modifier = Modifier.weight(1f),
                    )
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
                        Text(AlarmSounds.labelFor(context, soundUri))
                    }
                    TextButton(onClick = { showSoundPicker = true }) { Text("Cambiar") }
                }
            }

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
                        FilterChip(selected = difficulty == value, onClick = { difficulty = value }, label = { Text(text) })
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Vibrar")
                Switch(checked = vibrate, onCheckedChange = { vibrate = it })
            }

            Text(
                "Te avisaremos 1 hora antes con una notificación silenciosa (puedes apagar solo esa vez desde ahí).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 8.dp),
            )

            Button(
                onClick = {
                    viewModel.save(
                        label = label,
                        hour = hour,
                        minute = minute,
                        repeatDaysBitmask = repeatBitmask,
                        challenge = challenge,
                        difficulty = difficulty,
                        vibrate = vibrate,
                        soundUri = soundUri,
                        preAlarmMinutesBefore = 60,
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
        AlarmSoundPickerDialog(
            currentUri = soundUri,
            onDismiss = { showSoundPicker = false },
            onSelect = { soundUri = it; showSoundPicker = false },
        )
    }
}
