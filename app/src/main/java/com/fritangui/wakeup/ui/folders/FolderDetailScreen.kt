@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.folders

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DIAS_CORTOS = listOf("L", "M", "X", "J", "V", "S", "D")

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
    val alarms by viewModel.alarms.collectAsState()
    val isPinned by viewModel.isPinned.collectAsState()
    var tabIndex by remember { mutableIntStateOf(0) }
    val isReadOnly = folder?.isActive != true
    val tabs = listOf("Materias", "Tareas", "Alarmas")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(folder?.name ?: "") },
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
                    0 -> SubjectsTab(subjects) { onOpenSubject(viewModel.folderId, it) }
                    1 -> TasksTab(tasks, onToggle = viewModel::setTaskCompleted) { onOpenTask(viewModel.folderId, it) }
                    else -> AlarmsTab(alarms, readOnly = isReadOnly, onToggle = viewModel::setAlarmEnabled) {
                        onOpenAlarm(viewModel.folderId, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectsTab(subjects: List<SubjectWithSessions>, onClick: (Long) -> Unit) {
    if (subjects.isEmpty()) {
        EmptyState("Agrega tus materias con sus horarios y salones")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(subjects, key = { it.subject.id }) { entry ->
            Card(modifier = Modifier.fillMaxWidth().animateItem().padding(vertical = 6.dp).clickable { onClick(entry.subject.id) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(14.dp).background(Color(entry.subject.colorArgb), CircleShape))
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(entry.subject.name, style = MaterialTheme.typography.titleMedium)
                        if (entry.sessions.isEmpty()) {
                            Text("Sin horario asignado", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            entry.sessions.sortedBy { it.dayOfWeek }.forEach { s ->
                                Text(
                                    "${DIAS_CORTOS[s.dayOfWeek - 1]} ${formatMinutes(s.startMinuteOfDay)}–${formatMinutes(s.endMinuteOfDay)} · ${s.room}",
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TasksTab(tasks: List<TaskEntity>, onToggle: (Long, Boolean) -> Unit, onClick: (Long) -> Unit) {
    if (tasks.isEmpty()) {
        EmptyState("Agrega tareas con o sin fecha de vencimiento")
        return
    }
    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(tasks, key = { it.id }) { task ->
            Card(modifier = Modifier.fillMaxWidth().animateItem().padding(vertical = 6.dp).clickable { onClick(task.id) }) {
                Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = task.isCompleted, onCheckedChange = { onToggle(task.id, it) })
                    Column {
                        Text(
                            task.title,
                            style = MaterialTheme.typography.titleMedium,
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        )
                        Text(
                            task.dueAtEpochMillis?.let { "Vence " + formatDueDate(it) } ?: "Sin fecha de vencimiento",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
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
                    Column {
                        Text(
                            "%02d:%02d".format(alarm.hour, alarm.minute),
                            style = MaterialTheme.typography.headlineMedium,
                        )
                        Text(alarm.label.ifBlank { "Alarma" }, style = MaterialTheme.typography.bodyMedium)
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

private fun formatDueDate(epochMillis: Long): String {
    val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
    return "%02d/%02d %02d:%02d".format(dt.dayOfMonth, dt.monthNumber, dt.hour, dt.minute)
}
