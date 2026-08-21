package com.fritangui.wakeup.ui.tasks

import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.ui.components.AppTimePickerDialog
import com.fritangui.wakeup.ui.components.ClockTimeText
import com.fritangui.wakeup.ui.components.continueListFormat
import com.fritangui.wakeup.ui.navigation.UnsavedChangesGuard
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
    // TextFieldValue (no solo el String) para poder mover el cursor a mano al auto-continuar o
    // salir de una lista con viñetas/numerada — ver continueListFormat.
    var descriptionField by rememberSaveable(task?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(task?.description ?: ""))
    }
    var notesField by rememberSaveable(task?.id, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(task?.notes ?: ""))
    }
    var isNoteImportant by rememberSaveable(task?.id) { mutableStateOf(task?.isNoteImportant ?: false) }
    // Cuánto vale esta tarea en la nota final; se puede dejar sin llenar acá y confirmarlo/editarlo
    // al marcarla como completada más adelante. Se calcula una sola vez (no dentro de la lambda de
    // rememberSaveable) para poder reusar el mismo valor también al comparar cambios sin guardar.
    val initialWeightPercent = remember(task?.id) {
        task?.gradeWeightPercent?.let { if (it == it.toLong().toDouble()) it.toLong().toString() else it.toString() } ?: ""
    }
    var weightPercent by rememberSaveable(task?.id) { mutableStateOf(initialWeightPercent) }
    // Para una tarea nueva, se preselecciona la última materia usada en esta misma carpeta
    // (ver TaskCreationSessionState) — se pierde si cambias de carpeta o cierras la app.
    val initialSubjectId = remember(task?.id) { task?.subjectId ?: viewModel.initialSubjectIdForNew }
    var selectedSubjectId by rememberSaveable(task?.id) { mutableStateOf(initialSubjectId) }
    var hasDueDate by rememberSaveable(task?.id) { mutableStateOf(task?.dueAtEpochMillis != null) }
    var dueAtMillis by rememberSaveable(task?.id) { mutableStateOf(task?.dueAtEpochMillis) }
    // 11:59pm del mismo día por defecto (lo más común: "entregar antes de que acabe el día"), pero
    // editable — se guarda aparte de la fecha para no perderlo si el usuario solo cambia la fecha.
    val initialDueHour = remember(task?.id) { task?.dueAtEpochMillis?.let { epochToLocalDateTime(it).hour } ?: 23 }
    val initialDueMinute = remember(task?.id) { task?.dueAtEpochMillis?.let { epochToLocalDateTime(it).minute } ?: 59 }
    var dueHour by rememberSaveable(task?.id) { mutableStateOf(initialDueHour) }
    var dueMinute by rememberSaveable(task?.id) { mutableStateOf(initialDueMinute) }
    val initialReminderWeekBefore = remember(task?.id) { task?.reminderOffsetsMinutes?.contains(7 * 24 * 60L) ?: true }
    val initialReminderDayBefore = remember(task?.id) { task?.reminderOffsetsMinutes?.contains(24 * 60L) ?: true }
    var reminderWeekBefore by rememberSaveable(task?.id) { mutableStateOf(initialReminderWeekBefore) }
    var reminderDayBefore by rememberSaveable(task?.id) { mutableStateOf(initialReminderDayBefore) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var subjectMenuExpanded by remember { mutableStateOf(false) }
    var showNoSubjectConfirm by remember { mutableStateOf(false) }
    var dontAskAgainChecked by remember { mutableStateOf(false) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Cualquier campo distinto de lo que llegó de la BD (o, para una tarea nueva, distinto del
    // valor por defecto) cuenta como cambio sin guardar.
    val isDirty = title != (task?.title ?: "") ||
        descriptionField.text != (task?.description ?: "") ||
        notesField.text != (task?.notes ?: "") ||
        isNoteImportant != (task?.isNoteImportant ?: false) ||
        weightPercent != initialWeightPercent ||
        selectedSubjectId != initialSubjectId ||
        hasDueDate != (task?.dueAtEpochMillis != null) ||
        dueAtMillis != task?.dueAtEpochMillis ||
        dueHour != initialDueHour ||
        dueMinute != initialDueMinute ||
        reminderWeekBefore != initialReminderWeekBefore ||
        reminderDayBefore != initialReminderDayBefore

    fun requestLeave(action: () -> Unit) {
        if (isDirty) {
            pendingLeaveAction = action
            confirmDiscard = true
        } else {
            action()
        }
    }
    fun tryExit() = requestLeave(onBack)

    BackHandler(onBack = ::tryExit)
    // Puente con la barra de navegación inferior: si se toca otro tab con cambios sin guardar acá,
    // deja que este mismo diálogo se muestre primero (#147).
    DisposableEffect(isDirty) {
        UnsavedChangesGuard.register(isDirty, ::requestLeave)
        onDispose { UnsavedChangesGuard.clear() }
    }

    fun doSave() {
        val offsets = buildList {
            if (reminderWeekBefore) add(7 * 24 * 60L)
            if (reminderDayBefore) add(24 * 60L)
        }
        viewModel.save(
            title = title,
            description = descriptionField.text,
            notes = notesField.text,
            isNoteImportant = isNoteImportant,
            subjectId = selectedSubjectId,
            dueAtEpochMillis = if (hasDueDate) dueAtMillis else null,
            reminderOffsetsMinutes = offsets,
            gradeWeightPercent = weightPercent.toDoubleOrNull(),
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
                navigationIcon = { IconButton(onClick = ::tryExit) { Icon(Icons.Default.ArrowBack, null) } },
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
                value = descriptionField,
                onValueChange = { descriptionField = continueListFormat(descriptionField, it) },
                label = { Text("Descripción (opcional). Admite listas con \"- \" o \"1. \"") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = notesField,
                onValueChange = { notesField = continueListFormat(notesField, it) },
                label = { Text("Notas (opcional)") },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = weightPercent,
                onValueChange = { weightPercent = it.filter { c -> c.isDigit() || c == '.' } },
                label = { Text("% que vale en la nota final (opcional)") },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            if (notesField.text.isNotBlank()) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { showDatePicker = true }) { Text(label) }
                    TextButton(onClick = { showTimePicker = true }) {
                        ClockTimeText(dueHour, dueMinute, style = MaterialTheme.typography.bodyLarge)
                    }
                }

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
                        // El picker trabaja en UTC a medianoche; se combina con la hora ya elegida
                        // (23:59 por defecto, ver arriba, o la que se haya puesto a mano).
                        val date = Instant.fromEpochMilliseconds(pickedUtcMidnight).toLocalDateTime(TimeZone.UTC).date
                        dueAtMillis = date.atTime(dueHour, dueMinute).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } },
        ) {
            DatePicker(state = state)
        }
    }

    if (showTimePicker) {
        AppTimePickerDialog(
            initialHour = dueHour,
            initialMinute = dueMinute,
            onDismiss = { showTimePicker = false },
            onConfirm = { h, m ->
                dueHour = h
                dueMinute = m
                // Si ya había una fecha elegida, la conserva y solo cambia la hora.
                val date = dueAtMillis?.let { epochToLocalDateTime(it).date }
                    ?: kotlinx.datetime.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
                dueAtMillis = date.atTime(h, m).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
                showTimePicker = false
            },
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Tienes cambios sin guardar en esta tarea. Si sales ahora se pierden.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; pendingLeaveAction?.invoke() }) { Text("Salir sin guardar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } },
        )
    }
}

private fun epochToLocalDateTime(epochMillis: Long) =
    Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())

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
