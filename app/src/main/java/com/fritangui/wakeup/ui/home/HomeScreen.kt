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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.ui.components.LocalUse24HourFormat
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import com.fritangui.wakeup.ui.theme.WakeUpPrimary
import com.fritangui.wakeup.ui.theme.WakeUpSecondary
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
            item {
                // Misma "tarjeta" completa que ya se ve en los widgets de home screen (fondo propio +
                // encabezado con punto de color), no solo las filas de adentro — así Inicio no se
                // siente como una versión recortada de lo que ya hay en el widget.
                WidgetStyleCard(
                    title = "Próximas clases",
                    dotColor = WakeUpPrimary,
                    isEmpty = nextClasses.isEmpty(),
                    emptyText = "No hay clases próximas",
                ) {
                    var lastDay: Int? = null
                    nextClasses.forEach { occurrence ->
                        val day = occurrence.start.dayOfWeek.value
                        if (day != lastDay) {
                            lastDay = day
                            DayHeader(DIA_LARGO[day - 1])
                        }
                        ClassRow(occurrence)
                    }
                }
            }
            item {
                WidgetStyleCard(
                    title = "Próximas tareas",
                    dotColor = WakeUpSecondary,
                    isEmpty = upcomingTasks.isEmpty(),
                    emptyText = "No hay tareas próximas",
                    topPadding = 16.dp,
                ) {
                    upcomingTasks.forEach { task ->
                        TaskRow(task, subjectColorsById[task.subjectId], task.subjectId?.let { subjectNamesById[it] })
                    }
                }
            }
        }
    }
}

/**
 * Misma "tarjeta" que arman los widgets de home screen: fondo propio con esquinas redondeadas y un
 * encabezado con un punto de color + título en negrilla, en vez de un simple `Text` de sección
 * flotando sobre el fondo de la pantalla.
 */
@Composable
private fun WidgetStyleCard(
    title: String,
    dotColor: Color,
    isEmpty: Boolean,
    emptyText: String,
    topPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
            Text(
                title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        if (isEmpty) {
            Text(
                emptyText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
            )
        } else {
            Column(modifier = Modifier.padding(bottom = 12.dp), content = content)
        }
    }
}

@Composable
private fun DayHeader(label: String) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
    )
}

@Composable
private fun ItemRow(accentColor: Color, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
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
    ItemRow(accentColor = Color(occurrence.colorArgb)) {
        Text(occurrence.subjectName, style = MaterialTheme.typography.titleMedium)
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

@Composable
private fun TaskRow(task: TaskEntity, subjectColorArgb: Int?, subjectName: String?) {
    val use24Hour = LocalUse24HourFormat.current
    ItemRow(accentColor = subjectColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary) {
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
