package com.fritangui.wakeup.ui.tasks

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlinx.datetime.toLocalDateTime

private val MESES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

private fun monthGroupLabel(epochMillis: Long?): String {
    if (epochMillis == null) return "Sin fecha"
    val date = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
    val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    val label = MESES[date.monthNumber - 1]
    return if (date.year != today.year) "$label ${date.year}" else label
}

private fun formatDueDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "Vence ${dt.dayOfMonth} ${MESES[dt.monthNumber - 1].take(3).lowercase()} · %02d:%02d".format(dt.hour, dt.minute)
}

/**
 * Agrega a un [LazyListScope] las tareas ya agrupadas: las pendientes primero, separadas por mes
 * de vencimiento (las sin fecha van al final de las pendientes, ver TaskDao.observeUpcoming), y
 * las completadas al final del todo bajo su propio encabezado "Completadas" — en vez de mezcladas
 * cronológicamente entre las demás, como pedía la carpeta y también cada materia por separado.
 */
fun LazyListScope.taskListItems(
    tasks: List<TaskEntity>,
    subjectColorsById: Map<Long, Int> = emptyMap(),
    subjectNamesById: Map<Long, String> = emptyMap(),
    showSubjectName: Boolean = false,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit,
    onDelete: ((TaskEntity) -> Unit)? = null,
    onCompleteWithGrade: ((TaskEntity, Double?, Double?) -> Unit)? = null,
) {
    val (completed, pending) = tasks.partition { it.isCompleted }

    var lastGroup: String? = null
    pending.forEach { task ->
        val group = monthGroupLabel(task.dueAtEpochMillis)
        if (group != lastGroup) {
            lastGroup = group
            item(key = "header_$group") { TaskGroupHeader(group) }
        }
        item(key = "task_${task.id}") {
            TaskListRow(
                task = task,
                subjectColorArgb = subjectColorsById[task.subjectId],
                subjectName = if (showSubjectName) subjectNamesById[task.subjectId] else null,
                onToggle = onToggle,
                onClick = onClick,
                onDelete = onDelete,
                onCompleteWithGrade = onCompleteWithGrade,
            )
        }
    }

    if (completed.isNotEmpty()) {
        item(key = "header_completadas") { TaskGroupHeader("Completadas") }
        completed.forEach { task ->
            item(key = "task_${task.id}") {
                TaskListRow(
                    task = task,
                    subjectColorArgb = subjectColorsById[task.subjectId],
                    subjectName = if (showSubjectName) subjectNamesById[task.subjectId] else null,
                    onToggle = onToggle,
                    onClick = onClick,
                    onDelete = onDelete,
                    onCompleteWithGrade = onCompleteWithGrade,
                )
            }
        }
    }
}

/**
 * Misma agrupación que [taskListItems] pero como una lista fija (no lazy) dentro de un [Column]
 * normal — para cuando ya está dentro de otro contenedor con scroll propio (p.ej. la pantalla de
 * materia, que ya es un Column con verticalScroll) y anidar otro LazyColumn ahí rompería el layout.
 */
@Composable
fun TaskListColumn(
    tasks: List<TaskEntity>,
    subjectColorsById: Map<Long, Int> = emptyMap(),
    subjectNamesById: Map<Long, String> = emptyMap(),
    showSubjectName: Boolean = false,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit,
    onDelete: ((TaskEntity) -> Unit)? = null,
    onCompleteWithGrade: ((TaskEntity, Double?, Double?) -> Unit)? = null,
) {
    val (completed, pending) = tasks.partition { it.isCompleted }
    Column {
        var lastGroup: String? = null
        pending.forEach { task ->
            val group = monthGroupLabel(task.dueAtEpochMillis)
            if (group != lastGroup) {
                lastGroup = group
                TaskGroupHeader(group)
            }
            TaskListRow(
                task = task,
                subjectColorArgb = subjectColorsById[task.subjectId],
                subjectName = if (showSubjectName) subjectNamesById[task.subjectId] else null,
                onToggle = onToggle,
                onClick = onClick,
                onDelete = onDelete,
                onCompleteWithGrade = onCompleteWithGrade,
            )
        }
        if (completed.isNotEmpty()) {
            TaskGroupHeader("Completadas")
            completed.forEach { task ->
                TaskListRow(
                    task = task,
                    subjectColorArgb = subjectColorsById[task.subjectId],
                    subjectName = if (showSubjectName) subjectNamesById[task.subjectId] else null,
                    onToggle = onToggle,
                    onClick = onClick,
                    onDelete = onDelete,
                    onCompleteWithGrade = onCompleteWithGrade,
                )
            }
        }
    }
}

@Composable
private fun TaskGroupHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListRow(
    task: TaskEntity,
    subjectColorArgb: Int?,
    subjectName: String?,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit,
    onDelete: ((TaskEntity) -> Unit)?,
    onCompleteWithGrade: ((TaskEntity, Double?, Double?) -> Unit)? = null,
) {
    val outline = MaterialTheme.colorScheme.outline
    // Completada: todo en gris (barra incluida) para que de verdad se sienta "apagada", no solo el texto tachado.
    val barColor = if (task.isCompleted) outline else subjectColorArgb?.let { Color(it) } ?: outline
    val textColor = if (task.isCompleted) outline else MaterialTheme.colorScheme.onSurface
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var showGradeDialog by remember { mutableStateOf(false) }

    Box {
        Card(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).let {
                if (onDelete != null) {
                    it.combinedClickable(onClick = { onClick(task.id) }, onLongClick = { showMenu = true })
                } else {
                    it.clickable { onClick(task.id) }
                }
            },
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ColorBar(barColor)
                Row(modifier = Modifier.fillMaxWidth().padding(end = 8.dp, top = 4.dp, bottom = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = task.isCompleted,
                        onCheckedChange = { checked ->
                            // Solo se pregunta la nota/% al COMPLETAR, no al desmarcar — nadie
                            // quiere que le pidan esos datos de nuevo solo por destildar por error.
                            if (checked && onCompleteWithGrade != null) {
                                showGradeDialog = true
                            } else {
                                onToggle(task.id, checked)
                            }
                        },
                    )
                    Column {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = textColor,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        )
                        Text(
                            task.dueAtEpochMillis?.let { formatDueDate(it) } ?: "Sin fecha de vencimiento",
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (task.isCompleted) outline else MaterialTheme.colorScheme.outline,
                        )
                        if (subjectName != null) {
                            Text(subjectName, style = MaterialTheme.typography.bodySmall, color = outline)
                        }
                        val gradeSummary = gradeSummary(task)
                        if (gradeSummary != null) {
                            Text(gradeSummary, style = MaterialTheme.typography.bodySmall, color = outline)
                        }
                    }
                }
            }
        }
        if (onDelete != null) {
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                DropdownMenuItem(
                    text = { Text("Eliminar tarea") },
                    onClick = { showMenu = false; confirmDelete = true },
                )
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar \"${task.title}\"?") },
            text = { Text("Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete?.invoke(task) }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }

    if (showGradeDialog) {
        CompleteWithGradeDialog(
            initialWeightPercent = task.gradeWeightPercent,
            onDismiss = { showGradeDialog = false },
            onConfirm = { grade, weight ->
                showGradeDialog = false
                onCompleteWithGrade?.invoke(task, grade, weight)
            },
        )
    }
}

@Composable
private fun CompleteWithGradeDialog(
    initialWeightPercent: Double?,
    onDismiss: () -> Unit,
    onConfirm: (gradeValue: Double?, gradeWeightPercent: Double?) -> Unit,
) {
    var gradeText by remember { mutableStateOf("") }
    var weightText by remember { mutableStateOf(initialWeightPercent?.let(::formatNumber) ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Marcar como completada") },
        text = {
            Column {
                Text(
                    "Puedes anotar la nota que sacaste y cuánto valía en la nota final (los dos son opcionales).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.outline,
                )
                OutlinedTextField(
                    value = gradeText,
                    onValueChange = { gradeText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Nota obtenida") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("% que valía") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(gradeText.toDoubleOrNull(), weightText.toDoubleOrNull()) }) {
                Text("Completar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

/** "Nota: 4.5 · 15%", o solo la mitad que haya, o null si no hay nada que mostrar. */
private fun gradeSummary(task: TaskEntity): String? {
    if (task.gradeValue == null && task.gradeWeightPercent == null) return null
    return buildString {
        if (task.gradeValue != null) append("Nota: ${formatNumber(task.gradeValue)}")
        if (task.gradeWeightPercent != null) {
            if (isNotEmpty()) append(" · ")
            append("${formatNumber(task.gradeWeightPercent)}%")
        }
    }
}

/** "4.5" en vez de "4.5000...", y "15" en vez de "15.0" cuando es un número entero. */
private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()

@Composable
private fun ColorBar(color: Color) {
    Box(modifier = Modifier.width(4.dp).height(56.dp).background(color))
}
