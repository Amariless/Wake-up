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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.ui.components.WheelTimePicker
import com.fritangui.wakeup.ui.theme.FolderColorPalette

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
                title = {
                    Text(
                        if (currentSubjectId == null) "Nueva materia" else "Editar materia",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(
                        onClick = { viewModel.saveBasicInfo(name, professor, selectedColor) {} },
                        enabled = name.isNotBlank(),
                    ) { Text(if (currentSubjectId == null) "Crear" else "Guardar") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Información básica", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la materia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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

            if (currentSubjectId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Horarios", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { editingSession = null; showSessionDialog = true }) { Text("+ Agregar") }
                }
                if (sessions.isEmpty()) {
                    Text(
                        "Aún no tiene horario. Puedes agregar el mismo horario para varios días a la vez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    sessions.forEach { session ->
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
            onSave = { newSessions ->
                newSessions.forEach { viewModel.addOrUpdateSession(it) }
                showSessionDialog = false
            },
        )
    }
}

@Composable
private fun SessionEditorDialog(
    initial: ClassSessionEntity?,
    onDismiss: () -> Unit,
    onSave: (List<ClassSessionEntity>) -> Unit,
) {
    // Al crear un horario nuevo se pueden elegir varios días a la vez (misma hora/salón, p.ej. una
    // materia que se ve martes y jueves); al editar uno ya existente se queda fijo a ese día.
    var selectedDays by remember { mutableStateOf(setOf(initial?.dayOfWeek ?: 1)) }
    var startHour by remember { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) / 60) }
    var startMinute by remember { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) % 60) }
    var endHour by remember { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) / 60) }
    var endMinute by remember { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) % 60) }
    var room by rememberSaveable { mutableStateOf(initial?.room ?: "") }

    // Al mover la hora de inicio (por interacción del usuario, no al abrir el diálogo),
    // la de fin se adelanta sola 2 horas — se puede seguir ajustando a mano después.
    fun applyStart(newHour: Int, newMinute: Int) {
        startHour = newHour
        startMinute = newMinute
        val totalEnd = (newHour * 60 + newMinute + 120) % (24 * 60)
        endHour = totalEnd / 60
        endMinute = totalEnd % 60
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Nuevo horario" else "Editar horario") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(if (initial == null) "Día(s)" else "Día")
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    DIA_NOMBRES.forEachIndexed { index, label ->
                        val iso = index + 1
                        FilterChip(
                            selected = iso in selectedDays,
                            onClick = {
                                selectedDays = if (initial != null) {
                                    setOf(iso)
                                } else if (iso in selectedDays) {
                                    (selectedDays - iso).ifEmpty { setOf(iso) }
                                } else {
                                    selectedDays + iso
                                }
                            },
                            label = { Text(label.take(1)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Text("Inicio", modifier = Modifier.padding(top = 8.dp))
                WheelTimePicker(
                    hour = startHour,
                    minute = startMinute,
                    onHourChange = { applyStart(it, startMinute) },
                    onMinuteChange = { applyStart(startHour, it) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Text("Fin", modifier = Modifier.padding(top = 8.dp))
                WheelTimePicker(
                    hour = endHour,
                    minute = endMinute,
                    onHourChange = { endHour = it },
                    onMinuteChange = { endMinute = it },
                    modifier = Modifier.fillMaxWidth(),
                )
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
                    val newSessions = selectedDays.sorted().map { day ->
                        ClassSessionEntity(
                            id = if (initial != null && day == initial.dayOfWeek) initial.id else 0L,
                            subjectId = initial?.subjectId ?: 0L,
                            dayOfWeek = day,
                            startMinuteOfDay = startHour * 60 + startMinute,
                            endMinuteOfDay = endHour * 60 + endMinute,
                            room = room.trim(),
                        )
                    }
                    onSave(newSessions)
                },
                enabled = room.isNotBlank() && selectedDays.isNotEmpty() && (endHour * 60 + endMinute) > (startHour * 60 + startMinute),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
