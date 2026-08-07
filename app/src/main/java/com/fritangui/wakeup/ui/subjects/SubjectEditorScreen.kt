@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.ui.theme.FolderColorPalette
import androidx.compose.ui.graphics.toArgb

private val DIA_NOMBRES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@Composable
fun SubjectEditorScreen(
    onBack: () -> Unit,
    viewModel: SubjectEditorViewModel = hiltViewModel(),
) {
    val subject by viewModel.subject.collectAsState()
    val currentSubjectId by viewModel.currentSubjectId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    var name by rememberSaveable(subject?.id) { mutableStateOf(subject?.name ?: "") }
    var professor by rememberSaveable(subject?.id) { mutableStateOf(subject?.professor ?: "") }
    var selectedColor by remember(subject?.id) { mutableStateOf(subject?.colorArgb ?: FolderColorPalette.first().toArgb()) }
    var showSessionDialog by remember { mutableStateOf(false) }
    var editingSession by remember { mutableStateOf<ClassSessionEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (currentSubjectId == null) "Nueva materia" else "Editar materia") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la materia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = professor,
                onValueChange = { professor = it },
                label = { Text("Profesor (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text("Color", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FolderColorPalette.forEach { color ->
                    val argb = color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (argb == selectedColor) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { selectedColor = argb },
                    )
                }
            }
            Button(
                onClick = { viewModel.saveBasicInfo(name, professor, selectedColor) {} },
                enabled = name.isNotBlank(),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) { Text(if (currentSubjectId == null) "Crear materia" else "Guardar cambios") }

            if (currentSubjectId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Horarios", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { editingSession = null; showSessionDialog = true }) { Text("+ Agregar") }
                }
                LazyColumn(modifier = Modifier.padding(top = 8.dp)) {
                    items(sessions, key = { it.id }) { session ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp).clickable {
                                    editingSession = session
                                    showSessionDialog = true
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${DIA_NOMBRES[session.dayOfWeek - 1]} · " +
                                        "%02d:%02d–%02d:%02d".format(
                                            session.startMinuteOfDay / 60, session.startMinuteOfDay % 60,
                                            session.endMinuteOfDay / 60, session.endMinuteOfDay % 60,
                                        ) + " · ${session.room}",
                                )
                                IconButton(onClick = { viewModel.deleteSession(session) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar horario")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showSessionDialog) {
        SessionEditorDialog(
            initial = editingSession,
            onDismiss = { showSessionDialog = false },
            onSave = {
                viewModel.addOrUpdateSession(it)
                showSessionDialog = false
            },
        )
    }
}

@Composable
private fun SessionEditorDialog(
    initial: ClassSessionEntity?,
    onDismiss: () -> Unit,
    onSave: (ClassSessionEntity) -> Unit,
) {
    var dayOfWeek by remember { mutableStateOf(initial?.dayOfWeek ?: 1) }
    var startHour by remember { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) / 60) }
    var startMinute by remember { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) % 60) }
    var endHour by remember { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) / 60) }
    var endMinute by remember { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) % 60) }
    var room by rememberSaveable { mutableStateOf(initial?.room ?: "") }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo horario" else "Editar horario") },
        text = {
            Column {
                Text("Día")
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(vertical = 8.dp)) {
                    DIA_NOMBRES.forEachIndexed { index, label ->
                        val iso = index + 1
                        FilterChip(selected = dayOfWeek == iso, onClick = { dayOfWeek = iso }, label = { Text(label) })
                    }
                }
                TimeRow("Inicio", startHour, startMinute, onHourChange = { startHour = it }, onMinuteChange = { startMinute = it })
                TimeRow("Fin", endHour, endMinute, onHourChange = { endHour = it }, onMinuteChange = { endMinute = it })
                OutlinedTextField(
                    value = room,
                    onValueChange = { room = it },
                    label = { Text("Salón") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        ClassSessionEntity(
                            id = initial?.id ?: 0L,
                            subjectId = initial?.subjectId ?: 0L,
                            dayOfWeek = dayOfWeek,
                            startMinuteOfDay = startHour * 60 + startMinute,
                            endMinuteOfDay = endHour * 60 + endMinute,
                            room = room.trim(),
                        ),
                    )
                },
                enabled = room.isNotBlank() && (endHour * 60 + endMinute) > (startHour * 60 + startMinute),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun TimeRow(label: String, hour: Int, minute: Int, onHourChange: (Int) -> Unit, onMinuteChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
        Text(label, modifier = Modifier.padding(end = 8.dp))
        OutlinedButton(onClick = { onHourChange((hour + 1) % 24) }) { Text("%02d".format(hour)) }
        Text(" : ")
        OutlinedButton(onClick = { onMinuteChange((minute + 5) % 60) }) { Text("%02d".format(minute)) }
    }
}
