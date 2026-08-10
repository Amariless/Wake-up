package com.fritangui.wakeup.ui.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.material3.rememberDatePickerState
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
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorScreen(
    onBack: () -> Unit,
    viewModel: TaskEditorViewModel = hiltViewModel(),
) {
    val task by viewModel.task.collectAsState()
    val subjects by viewModel.subjectsInFolder.collectAsState()

    var title by rememberSaveable(task?.id) { mutableStateOf(task?.title ?: "") }
    var description by rememberSaveable(task?.id) { mutableStateOf(task?.description ?: "") }
    var notes by rememberSaveable(task?.id) { mutableStateOf(task?.notes ?: "") }
    var isNoteImportant by rememberSaveable(task?.id) { mutableStateOf(task?.isNoteImportant ?: false) }
    // Para una tarea nueva, se preselecciona la última materia usada en esta misma carpeta
    // (ver TaskCreationSessionState) — se pierde si cambias de carpeta o cierras la app.
    var selectedSubjectId by rememberSaveable(task?.id) {
        mutableStateOf(task?.subjectId ?: viewModel.initialSubjectIdForNew)
    }
    var hasDueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueAtEpochMillis != null) }
    var dueAtMillis by rememberSaveable(task?.id) { mutableStateOf(task?.dueAtEpochMillis) }
    var reminderWeekBefore by rememberSaveable(task?.id) {
        mutableStateOf(task?.reminderOffsetsMinutes?.contains(7 * 24 * 60L) ?: true)
    }
    var reminderDayBefore by rememberSaveable(task?.id) {
        mutableStateOf(task?.reminderOffsetsMinutes?.contains(24 * 60L) ?: true)
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var showNoSubjectConfirm by remember { mutableStateOf(false) }
    var dontAskAgainChecked by remember { mutableStateOf(false) }

    fun doSave() {
        val offsets = buildList {
            if (reminderWeekBefore) add(7 * 24 * 60L)
            if (reminderDayBefore) add(24 * 60L)
        }
        viewModel.save(
            title = title,
            description = description,
            notes = notes,
            isNoteImportant = isNoteImportant,
            subjectId = selectedSubjectId,
            dueAtEpochMillis = if (hasDueDate) dueAtMillis else null,
            reminderOffsetsMinutes = offsets,
            onSaved = onBack,
        )
    }

    fun onSaveClicked() {
        if (selectedSubjectId == null && !viewModel.skipNoSubjectConfirmation) {
            dontAskAgainChecked = false
            showNoSubjectConfirm = true
        } else {
            doSave()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isNew) "Nueva tarea" else "Editar tarea") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                actions = {
                    if (!viewModel.isNew) {
                        IconButton(onClick = { viewModel.delete(onBack) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar tarea")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Título") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Descripción (opcional)") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (notes.isNotBlank()) {
                Row(
                    modifier = Modifier.fillMaxWidth().clickable { isNoteImportant = !isNoteImportant },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = isNoteImportant, onCheckedChange = { isNoteImportant = it })
                    Column {
                        Text("Importante")
                        Text(
                            "La nota aparece también debajo de esta tarea en el widget",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }

            val selectedSubject = subjects.find { it.id == selectedSubjectId }
            ExposedDropdownMenuBox(
                expanded = subjectMenuExpanded,
                onExpandedChange = { subjectMenuExpanded = it },
                modifier = Modifier.padding(top = 8.dp),
            ) {
                OutlinedTextField(
                    value = selectedSubject?.name ?: "Sin materia asociada",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Materia (opcional)") },
                    leadingIcon = if (selectedSubject != null) {
                        { ColorDot(selectedSubject.colorArgb) }
                    } else {
                        null
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectMenuExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(),
                )
                ExposedDropdownMenu(
                    expanded = subjectMenuExpanded,
                    onDismissRequest = { subjectMenuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text("Sin materia asociada") },
                        onClick = { selectedSubjectId = null; subjectMenuExpanded = false },
                    )
                    subjects.forEach { subject ->
                        DropdownMenuItem(
                            text = { Text(subject.name) },
                            leadingIcon = { ColorDot(subject.colorArgb) },
                            onClick = { selectedSubjectId = subject.id; subjectMenuExpanded = false },
                        )
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Fecha de vencimiento", style = MaterialTheme.typography.titleMedium)
                Switch(
                    checked = hasDueDate,
                    onCheckedChange = {
                        hasDueDate = it
                        if (it && dueAtMillis == null) showDatePicker = true
                    },
                )
            }

            if (hasDueDate) {
                val label = dueAtMillis?.let { formatDate(it) } ?: "Elegir fecha"
                TextButton(onClick = { showDatePicker = true }) { Text(label) }

                Text("Recordarme", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 8.dp))
                ReminderToggleRow("1 semana antes", reminderWeekBefore) { reminderWeekBefore = it }
                ReminderToggleRow("1 día antes", reminderDayBefore) { reminderDayBefore = it }
            }

            Button(
                onClick = ::onSaveClicked,
                enabled = title.isNotBlank() && (!hasDueDate || dueAtMillis != null),
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
            ) { Text("Guardar") }
        }
    }

    if (showNoSubjectConfirm) {
        AlertDialog(
            onDismissRequest = { showNoSubjectConfirm = false },
            title = { Text("¿Guardar sin materia asignada?") },
            text = {
                Column {
                    Text("Esta tarea no quedará asociada a ninguna materia.")
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp).clickable {
                            dontAskAgainChecked = !dontAskAgainChecked
                        },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = dontAskAgainChecked, onCheckedChange = { dontAskAgainChecked = it })
                        Text("No preguntar de nuevo esta sesión")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (dontAskAgainChecked) viewModel.dontAskAgainThisSession()
                    showNoSubjectConfirm = false
                    doSave()
                }) { Text("Guardar de todos modos") }
            },
            dismissButton = { TextButton(onClick = { showNoSubjectConfirm = false }) { Text("Cancelar") } },
        )
    }

    if (showDatePicker) {
        val initialMillis = dueAtMillis ?: kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        val state = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { pickedUtcMidnight ->
                        // El picker trabaja en UTC a medianoche; lo llevamos a las 23:59 hora local de ese día.
                        val date = Instant.fromEpochMilliseconds(pickedUtcMidnight).toLocalDateTime(TimeZone.UTC).date
                        dueAtMillis = date.atTime(23, 59).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
private fun ColorDot(colorArgb: Int) {
    Box(modifier = Modifier.size(12.dp).clip(CircleShape).background(Color(colorArgb)))
}

@Composable
private fun ReminderToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun formatDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d/%02d/%04d".format(dt.dayOfMonth, dt.monthNumber, dt.year)
}
