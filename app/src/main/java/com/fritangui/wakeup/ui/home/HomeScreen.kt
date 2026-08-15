@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.WeeklyClassEntry
import com.fritangui.wakeup.domain.nextClassDayOfWeek
import com.fritangui.wakeup.ui.components.LocalUse24HourFormat
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import com.fritangui.wakeup.ui.theme.WakeUpPrimary
import com.fritangui.wakeup.ui.theme.WakeUpSecondary
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

private val DIA_LARGO = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val CARD_RADIUS = 20.dp

/** Filas planas de la tarjeta de "Próximas clases" — planas (no anidadas en un solo `item`) a
 *  propósito, para poder hacer scroll a una fila concreta con [androidx.compose.foundation.lazy.LazyListState] (#142). */
private sealed interface ClassCardRow {
    data class DayHeaderRow(val dayOfWeek: Int, val isToday: Boolean, val isNextClassDay: Boolean) : ClassCardRow
    data class ClassEntryRow(val entry: WeeklyClassEntry, val isOngoing: Boolean) : ClassCardRow
    data object EmptyRow : ClassCardRow
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSubject: (folderId: Long, subjectId: Long) -> Unit = { _, _ -> },
    onOpenTask: (folderId: Long, taskId: Long) -> Unit = { _, _ -> },
    /** Se incrementa cada vez que se toca el encabezado del widget de "Próximas clases" (#142): un
     *  nuevo valor (aunque ya se esté en Inicio) vuelve a disparar el scroll a la clase actual/próxima. */
    scrollToNextClassSignal: Int = 0,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val weeklyClassDays by viewModel.weeklyClassDays.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val subjectColorsById by viewModel.subjectColorsById.collectAsState()
    val subjectNamesById by viewModel.subjectNamesById.collectAsState()

    // Se calcula una sola vez al entrar (no hace falta que "hoy"/"ahora" cambien en vivo mientras
    // se mira Inicio) — igual que ya hacía la versión anterior de esta pantalla.
    val now = remember { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()) }
    val todayDayOfWeek = now.dayOfWeek.value
    val nowMinuteOfDay = now.hour * 60 + now.minute
    val nextClassDay = remember(weeklyClassDays) { nextClassDayOfWeek(weeklyClassDays, todayDayOfWeek) }

    // Filas planas de la tarjeta de clases: encabezado de día + cada clase de ese día, en orden
    // lunes→domingo (#140). Cada clase de HOY que ya está en curso ahora mismo queda marcada para
    // resaltarse más (reutilizado también por el widget, #143).
    val classCardRows = remember(weeklyClassDays, todayDayOfWeek, nextClassDay) {
        if (weeklyClassDays.isEmpty()) {
            listOf(ClassCardRow.EmptyRow)
        } else {
            weeklyClassDays.flatMap { day ->
                listOf(ClassCardRow.DayHeaderRow(day.dayOfWeek, day.dayOfWeek == todayDayOfWeek, day.dayOfWeek == nextClassDay)) +
                    day.classes.map { entry ->
                        val isOngoing = day.dayOfWeek == todayDayOfWeek && nowMinuteOfDay in entry.startMinuteOfDay until entry.endMinuteOfDay
                        ClassCardRow.ClassEntryRow(entry, isOngoing)
                    }
            }
        }
    }
    val listState = rememberLazyListState()
    // +1 fila por el título "Próximas clases" (índice 0), que también es un item plano de la lista.
    val scrollTargetIndex = remember(classCardRows, todayDayOfWeek, nowMinuteOfDay) {
        findScrollTargetIndex(classCardRows, todayDayOfWeek, nowMinuteOfDay)?.plus(1)
    }
    LaunchedEffect(scrollToNextClassSignal) {
        if (scrollToNextClassSignal > 0 && scrollTargetIndex != null) {
            listState.animateScrollToItem(scrollTargetIndex)
        }
    }

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
        if (weeklyClassDays.isEmpty() && upcomingTasks.isEmpty()) {
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
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp), state = listState) {
            item(key = "classes_title") {
                CardTitleRow("Próximas clases", WakeUpPrimary, roundedBottom = classCardRows.size <= 1 && classCardRows.firstOrNull() == ClassCardRow.EmptyRow)
            }
            itemsIndexed(classCardRows, key = { index, _ -> "class_row_$index" }) { index, row ->
                val isLast = index == classCardRows.lastIndex
                when (row) {
                    is ClassCardRow.EmptyRow -> CardEmptyText("No hay clases esta semana", roundedBottom = true)
                    is ClassCardRow.DayHeaderRow -> CardRowBackground(roundedBottom = false) {
                        DayHeader(DIA_LARGO[row.dayOfWeek - 1], row.isToday, row.isNextClassDay)
                    }
                    is ClassCardRow.ClassEntryRow -> CardRowBackground(roundedBottom = isLast, bottomExtraPadding = isLast) {
                        ClassRow(row.entry, row.isOngoing, onClick = { onOpenSubject(row.entry.folderId, row.entry.subjectId) })
                    }
                }
            }
            item(key = "tasks_title") {
                CardTitleRow("Próximas tareas", WakeUpSecondary, roundedBottom = upcomingTasks.isEmpty(), topPadding = 16.dp)
            }
            if (upcomingTasks.isEmpty()) {
                item(key = "tasks_empty") { CardEmptyText("No hay tareas próximas", roundedBottom = true) }
            } else {
                itemsIndexed(upcomingTasks, key = { _, task -> "task_row_${task.id}" }) { index, task ->
                    val isLast = index == upcomingTasks.lastIndex
                    CardRowBackground(roundedBottom = isLast, bottomExtraPadding = isLast) {
                        TaskRow(
                            task,
                            subjectColorsById[task.subjectId],
                            task.subjectId?.let { subjectNamesById[it] },
                            onClick = { onOpenTask(task.folderId, task.id) },
                        )
                    }
                }
            }
        }
    }
}

/**
 * Dentro de [rows] (ya en orden lunes→domingo), busca la clase que aplica AHORA MISMO: la que está
 * en curso si hay una, si no la próxima que todavía no empieza (hoy más tarde, o el primer día que
 * le sigue con clase) — usado tanto para el auto-scroll (#142) como para decidir qué resaltar más
 * (#143 ya se resuelve por fila con [ClassCardRow.ClassEntryRow.isOngoing], pero el índice de
 * scroll necesita esta misma noción de "próxima").
 */
private fun findScrollTargetIndex(rows: List<ClassCardRow>, todayDayOfWeek: Int, nowMinuteOfDay: Int): Int? {
    val ongoingIndex = rows.indexOfFirst { it is ClassCardRow.ClassEntryRow && it.isOngoing }
    if (ongoingIndex >= 0) return ongoingIndex

    val dayOrder = (0..6).map { offset -> ((todayDayOfWeek - 1 + offset) % 7) + 1 }
    for (day in dayOrder) {
        val headerIndex = rows.indexOfFirst { it is ClassCardRow.DayHeaderRow && it.dayOfWeek == day }
        if (headerIndex < 0) continue
        var i = headerIndex + 1
        while (i < rows.size) {
            val row = rows[i]
            if (row !is ClassCardRow.ClassEntryRow) break
            val notYetOver = day != todayDayOfWeek || row.entry.endMinuteOfDay > nowMinuteOfDay
            if (notYetOver) return i
            i++
        }
    }
    return null
}

@Composable
private fun CardTitleRow(title: String, dotColor: Color, roundedBottom: Boolean, topPadding: Dp = 0.dp) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = topPadding)
            .clip(cardShape(roundedTop = true, roundedBottom = roundedBottom))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(dotColor))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun CardEmptyText(text: String, roundedBottom: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape(roundedTop = false, roundedBottom = roundedBottom))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
    }
}

/** Fondo continuo de "tarjeta tipo widget" compartido fila a fila (ver [ClassCardRow]): mismo color
 *  en todas, esquinas redondeadas solo en la última fila de cada tarjeta. */
@Composable
private fun CardRowBackground(roundedBottom: Boolean, bottomExtraPadding: Boolean = false, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape(roundedTop = false, roundedBottom = roundedBottom))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(bottom = if (bottomExtraPadding) 12.dp else 0.dp),
    ) {
        content()
    }
}

private fun cardShape(roundedTop: Boolean, roundedBottom: Boolean) = RoundedCornerShape(
    topStart = if (roundedTop) CARD_RADIUS else 0.dp,
    topEnd = if (roundedTop) CARD_RADIUS else 0.dp,
    bottomStart = if (roundedBottom) CARD_RADIUS else 0.dp,
    bottomEnd = if (roundedBottom) CARD_RADIUS else 0.dp,
)

@Composable
private fun DayHeader(label: String, isToday: Boolean, isNextClassDay: Boolean) {
    // "Hoy" se resalta con una píldora de color propio; el próximo día con clase (si hoy no tiene
    // ninguna) se marca aparte pero con menos contraste (gris) — ver #140.
    when {
        isToday -> Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(WakeUpPrimary.copy(alpha = 0.22f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("$label · Hoy", style = MaterialTheme.typography.labelLarge, color = WakeUpPrimary, fontWeight = FontWeight.Bold)
        }
        isNextClassDay -> Box(
            modifier = Modifier
                .padding(start = 16.dp, end = 16.dp, top = 6.dp, bottom = 2.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text(
                "$label · Próxima",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.outline,
            )
        }
        else -> Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.outline,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
        )
    }
}

@Composable
private fun ItemRow(accentColor: Color, highlighted: Boolean = false, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
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
                // La clase que está pasando AHORA mismo se resalta con más contraste que el resto
                // (#143): fondo propio en vez del genérico de las demás filas.
                .background(if (highlighted) accentColor.copy(alpha = 0.20f) else MaterialTheme.colorScheme.surfaceContainerHigh)
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            content = content,
        )
    }
}

@Composable
private fun ClassRow(entry: WeeklyClassEntry, isOngoing: Boolean, onClick: () -> Unit) {
    val use24Hour = LocalUse24HourFormat.current
    ItemRow(accentColor = Color(entry.colorArgb), highlighted = isOngoing, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(entry.subjectName, style = MaterialTheme.typography.titleMedium)
            if (isOngoing) {
                Text(
                    " · Ahora",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(entry.colorArgb),
                    fontWeight = FontWeight.Bold,
                )
            }
        }
        Text(
            "${formatMinuteOfDay(entry.startMinuteOfDay, use24Hour)}–${formatMinuteOfDay(entry.endMinuteOfDay, use24Hour)} · ${entry.room}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
    }
}

private fun formatMinuteOfDay(minuteOfDay: Int, use24Hour: Boolean): String {
    val hour = minuteOfDay / 60
    val minute = minuteOfDay % 60
    return formatClockTime(hour, minute, use24Hour) + (amPmSuffix(hour, use24Hour)?.let { " $it" } ?: "")
}

@Composable
private fun TaskRow(task: TaskEntity, subjectColorArgb: Int?, subjectName: String?, onClick: () -> Unit) {
    val use24Hour = LocalUse24HourFormat.current
    ItemRow(accentColor = subjectColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary, onClick = onClick) {
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
