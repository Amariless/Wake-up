@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.fritangui.wakeup.ui.folders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.AlarmTiming
import com.fritangui.wakeup.ui.components.ClockTimeText
import com.fritangui.wakeup.ui.tasks.taskListItems
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

// 2 letras (no 1): con 1 sola letra miércoles y martes/mayo... quedaban indistinguibles ("X" para
// miércoles no es intuitivo, y varios días comparten inicial).
private val DIAS_CORTOS = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")

@Composable
fun FolderDetailScreen(
    onBack: () -> Unit,
    onOpenSubject: (folderId: Long, subjectId: Long) -> Unit,
    onOpenTask: (folderId: Long, taskId: Long) -> Unit,
    onOpenAlarm: (folderId: Long, alarmId: Long) -> Unit,
    viewModel: FolderDetailViewModel = hiltViewModel(),
) {
    val folder by viewModel.folder.collectAsState()
    val subjects by viewModel.subjects.collectAsState()
    val tasks by viewModel.tasks.collectAsState()
    val subjectNamesById by viewModel.subjectNamesById.collectAsState()
    val subjectColorsById by viewModel.subjectColorsById.collectAsState()
    val alarms by viewModel.alarms.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    // rememberSaveable a propósito: Navigation-Compose descompone esta pantalla por completo al
    // navegar a un editor y la vuelve a componer desde cero al volver (back), así que un simple
    // remember perdía la pestaña seleccionada y siempre volvía a "Materias".
    var tabIndex by rememberSaveable { mutableIntStateOf(0) }
    val isReadOnly = folder?.isActive != true
    val tabs = listOf("Materias", "Tareas", "Alarmas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Volver") }
                },
                actions = {
                    IconButton(onClick = viewModel::togglePinned) {
                        Icon(
                            if (isPinned) Icons.Default.Star else Icons.Outlined.StarOutline,
                            contentDescription = if (isPinned) "Quitar como carpeta principal" else "Marcar como carpeta principal",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            if (!isReadOnly) {
                FloatingActionButton(
                    onClick = {
                        when (tabIndex) {
                            0 -> onOpenSubject(viewModel.folderId, 0L)
                            1 -> onOpenTask(viewModel.folderId, 0L)
                            else -> onOpenAlarm(viewModel.folderId, 0L)
                        }
                    },
                ) { Icon(Icons.Default.Add, contentDescription = "Agregar") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                }
            }
            AnimatedContent(
                targetState = tabIndex,
                transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(120)) },
                label = "folder_tab",
            ) { index ->
                when (index) {
                    0 -> SubjectsTab(
                        subjects = subjects,
                        onClick = { onOpenSubject(viewModel.folderId, it) },
                        onDelete = viewModel::deleteSubject,
                    )
                    1 -> TasksTab(
                        tasks = tasks,
                        subjectNamesById = subjectNamesById,
                        subjectColorsById = subjectColorsById,
                        onToggle = viewModel::setTaskCompleted,
                        onClick = { onOpenTask(viewModel.folderId, it) },
                        onDelete = viewModel::deleteTask,
                    )
                    else -> AlarmsTab(alarms, readOnly = isReadOnly, onToggle = viewModel::setAlarmEnabled) {
                        onOpenAlarm(viewModel.folderId, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsTab(subjects: List<SubjectWithSessions>, onClick: (Long) -> Unit, onDelete: (SubjectEntity) -> Unit) {
    if (subjects.isEmpty()) {
        EmptyState("Agrega tus materias con sus horarios y salones")
        return
    }
    var menuFor by remember { mutableStateOf<SubjectWithSessions?>(null) }
    var confirmDeleteFor by remember { mutableStateOf<SubjectWithSessions?>(null) }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(subjects, key = { it.subject.id }) { entry ->
            Box(modifier = Modifier.animateItem()) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).combinedClickable(
                        onClick = { onClick(entry.subject.id) },
                        onLongClick = { menuFor = entry },
                    ),
                ) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).background(Color(entry.subject.colorArgb), CircleShape))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(entry.subject.name, style = MaterialTheme.typography.titleMedium)
                        if (entry.sessions.isEmpty()) {
                            Text("Sin horario asignado", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            val outline = MaterialTheme.colorScheme.outline
                            entry.sessions.sortedBy { it.dayOfWeek }.forEach { s ->
                                Text(
                                    buildAnnotatedString {
                                        withStyle(SpanStyle(color = outline)) { append(DIAS_CORTOS[s.dayOfWeek - 1]) }
                                        append(" ${formatMinutes(s.startMinuteOfDay)}–${formatMinutes(s.endMinuteOfDay)} · ")
                                        withStyle(SpanStyle(color = outline)) { append(s.room) }
                                    },
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
                }
                DropdownMenu(expanded = menuFor?.subject?.id == entry.subject.id, onDismissRequest = { menuFor = null }) {
                    DropdownMenuItem(
                        text = { Text("Eliminar materia") },
                        onClick = { menuFor = null; confirmDeleteFor = entry },
                    )
                }
            }
        }
    }

    confirmDeleteFor?.let { entry ->
        AlertDialog(
            onDismissRequest = { confirmDeleteFor = null },
            title = { Text("¿Eliminar \"${entry.subject.name}\"?") },
            text = { Text("También se borrará su horario. Las tareas que tenía asociadas no se borran, solo quedan sin materia.") },
            confirmButton = {
                TextButton(onClick = { confirmDeleteFor = null; onDelete(entry.subject) }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteFor = null }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun TasksTab(
    tasks: List<TaskEntity>,
    subjectNamesById: Map<Long, String>,
    subjectColorsById: Map<Long, Int>,
    onToggle: (Long, Boolean) -> Unit,
    onClick: (Long) -> Unit,
    onDelete: (TaskEntity) -> Unit,
) {
    if (tasks.isEmpty()) {
        EmptyState("Agrega tareas con o sin fecha de vencimiento")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        taskListItems(
            tasks = tasks,
            subjectColorsById = subjectColorsById,
            subjectNamesById = subjectNamesById,
            showSubjectName = true,
            onToggle = onToggle,
            onClick = onClick,
            onDelete = onDelete,
        )
    }
}

@Composable
private fun AlarmsTab(alarms: List<AlarmEntity>, readOnly: Boolean, onToggle: (Long, Boolean) -> Unit, onClick: (Long) -> Unit) {
    if (alarms.isEmpty()) {
        EmptyState("Agrega alarmas asociadas a esta carpeta")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(alarms, key = { it.id }) { alarm ->
            Card(modifier = Modifier.fillMaxWidth().animateItem().padding(vertical = 6.dp).clickable { onClick(alarm.id) }) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        // Distinto de una alarma real: un Recordatorio es solo una notificación,
                        // no algo que suene y haya que apagar (ver AlarmsListScreen para el mismo criterio).
                        Icon(
                            if (alarm.kind == AlarmKind.REMINDER) Icons.Default.Notifications else Icons.Default.Alarm,
                            contentDescription = null,
                            tint = if (alarm.kind == AlarmKind.REMINDER) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 12.dp),
                        )
                        Column {
                            ClockTimeText(alarm.hour, alarm.minute, style = MaterialTheme.typography.headlineMedium)
                            Text(
                                alarm.label.ifBlank { if (alarm.kind == AlarmKind.REMINDER) "Recordatorio" else "Alarma" },
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            val now = remember { Clock.System.now() }
                            val trigger = AlarmTiming.nextTrigger(alarm, now = now)
                            if (trigger != null) {
                                Text(
                                    "Faltan ${AlarmTiming.formatRemaining(trigger - now)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                    Switch(checked = alarm.isEnabled, enabled = !readOnly, onCheckedChange = { onToggle(alarm.id, it) })
                }
            }
        }
    }
}

@Composable
private fun EmptyState(message: String) {
    Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(message, style = MaterialTheme.typography.bodyLarge)
    }
}

private fun formatMinutes(minuteOfDay: Int): String = "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
