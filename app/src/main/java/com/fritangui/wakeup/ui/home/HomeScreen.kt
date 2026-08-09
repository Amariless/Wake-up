@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DIA_CORTO = listOf("lun", "mar", "mié", "jue", "vie", "sáb", "dom")

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val nextClasses by viewModel.nextClasses.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Wake up") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
    ) { padding ->
        if (nextClasses.isEmpty() && upcomingTasks.isEmpty()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Default.CalendarMonth,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    "Crea una carpeta con tus materias y tareas para ver tu resumen aquí",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(top = 16.dp),
                )
            }
            return@Scaffold
        }
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            item { SectionTitle("Próximas clases") }
            if (nextClasses.isEmpty()) {
                item { EmptyRow("No hay clases próximas") }
            } else {
                items(nextClasses) { ClassRow(it) }
            }
            item { SectionTitle("Próximas tareas", topPadding = 24.dp) }
            if (upcomingTasks.isEmpty()) {
                item { EmptyRow("No hay tareas próximas") }
            } else {
                items(upcomingTasks) { TaskRow(it) }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, topPadding: androidx.compose.ui.unit.Dp = 0.dp) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = topPadding, bottom = 8.dp))
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
}

@Composable
private fun AccentCard(accentColor: Color, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.width(4.dp).height(44.dp).clip(RoundedCornerShape(topEnd = 2.dp, bottomEnd = 2.dp)).background(accentColor))
            Column(modifier = Modifier.padding(12.dp)) { content() }
        }
    }
}

@Composable
private fun ClassRow(occurrence: UpcomingClassOccurrence) {
    AccentCard(accentColor = Color(occurrence.colorArgb)) {
        Text(occurrence.subjectName, style = MaterialTheme.typography.titleMedium)
        Text(
            "${DIA_CORTO[occurrence.start.dayOfWeek.value - 1]} %02d:%02d–%02d:%02d · ${occurrence.room}".format(
                occurrence.start.hour, occurrence.start.minute, occurrence.end.hour, occurrence.end.minute,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

@Composable
private fun TaskRow(task: TaskEntity) {
    AccentCard(accentColor = MaterialTheme.colorScheme.secondary) {
        Text(task.title, style = MaterialTheme.typography.titleMedium)
        val dueText = task.dueAtEpochMillis?.let {
            val dt = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
            "Vence %02d/%02d %02d:%02d".format(dt.dayOfMonth, dt.monthNumber, dt.hour, dt.minute)
        } ?: "Sin fecha de vencimiento"
        Text(dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}
