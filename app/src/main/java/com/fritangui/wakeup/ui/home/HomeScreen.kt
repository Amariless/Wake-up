@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.ui.components.LocalUse24HourFormat
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DIA_LARGO = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@Composable
fun HomeScreen(onOpenSettings: () -> Unit, viewModel: HomeViewModel = hiltViewModel()) {
    val nextClasses by viewModel.nextClasses.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val subjectColorsById by viewModel.subjectColorsById.collectAsState()
    val subjectNamesById by viewModel.subjectNamesById.collectAsState()

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
                // Igual que en el widget: agrupadas con un encabezado por día, en vez de una lista
                // plana donde había que fijarse en la fecha de cada tarjeta para saber a cuál día
                // pertenecía.
                var lastDay: Int? = null
                nextClasses.forEach { occurrence ->
                    val day = occurrence.start.dayOfWeek.value
                    if (day != lastDay) {
                        lastDay = day
                        item(key = "day_$day") { DayHeader(DIA_LARGO[day - 1]) }
                    }
                    item(key = "class_${occurrence.subjectId}_${occurrence.start}") { ClassRow(occurrence) }
                }
            }
            item { SectionTitle("Próximas tareas", topPadding = 24.dp) }
            if (upcomingTasks.isEmpty()) {
                item { EmptyRow("No hay tareas próximas") }
            } else {
                items(upcomingTasks, key = { it.id }) { task ->
                    TaskRow(task, subjectColorsById[task.subjectId], task.subjectId?.let { subjectNamesById[it] })
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, topPadding: Dp = 0.dp) {
    Text(text, style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = topPadding, bottom = 8.dp))
}

@Composable
private fun DayHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
    )
}

@Composable
private fun EmptyRow(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 8.dp))
}

/**
 * Mismo lenguaje visual que las filas de los widgets de home screen (barra de color de la materia
 * + tarjeta interna con esquinas redondeadas), para que Inicio no se sienta como una versión más
 * pobre de lo que ya se ve en el widget.
 */
@Composable
private fun DecoratedRow(accentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(44.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor),
        )
        Column(
            modifier = Modifier
                .padding(start = 10.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun ClassRow(occurrence: UpcomingClassOccurrence) {
    val use24Hour = LocalUse24HourFormat.current
    DecoratedRow(accentColor = Color(occurrence.colorArgb)) {
        Text(occurrence.subjectName, style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                "${formatClockTime(occurrence.start.hour, occurrence.start.minute, use24Hour)}" +
                    (amPmSuffix(occurrence.start.hour, use24Hour)?.let { " $it" } ?: "") +
                    "–${formatClockTime(occurrence.end.hour, occurrence.end.minute, use24Hour)}" +
                    (amPmSuffix(occurrence.end.hour, use24Hour)?.let { " $it" } ?: "") +
                    " · ${occurrence.room}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, subjectColorArgb: Int?, subjectName: String?) {
    val use24Hour = LocalUse24HourFormat.current
    DecoratedRow(accentColor = subjectColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary) {
        Text(task.title, style = MaterialTheme.typography.titleMedium)
        val dueText = task.dueAtEpochMillis?.let {
            val dt = Instant.fromEpochMilliseconds(it).toLocalDateTime(TimeZone.currentSystemDefault())
            val time = formatClockTime(dt.hour, dt.minute, use24Hour)
            val suffix = amPmSuffix(dt.hour, use24Hour)?.let { s -> " $s" } ?: ""
            "Vence %02d/%02d %s%s".format(dt.dayOfMonth, dt.monthNumber, time, suffix)
        } ?: "Sin fecha de vencimiento"
        Text(dueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        // Menos contraste que la fecha, igual que en el widget: es información secundaria.
        if (subjectName != null) {
            Text(
                subjectName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
            )
        }
    }
}
