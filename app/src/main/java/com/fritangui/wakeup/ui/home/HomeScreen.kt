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
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.filled.AddTask
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fritangui.wakeup.ui.components.WakeUpTopBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.domain.AlarmTiming
import com.fritangui.wakeup.domain.TaskUrgencyBucket
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.domain.WeeklyClassEntry
import com.fritangui.wakeup.domain.nextClassDayOfWeek
import com.fritangui.wakeup.domain.taskUrgencyBucket
import com.fritangui.wakeup.permissions.AlarmVolumeStatus
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.ui.components.LocalUse24HourFormat
import com.fritangui.wakeup.ui.components.SubjectIndicator
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

private val DIA_LARGO = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
private val CARD_RADIUS = 16.dp

/** Filas planas de la tarjeta de "Próximas clases" — planas (no anidadas en un solo `item`) a
 *  propósito, para poder hacer scroll a una fila concreta con [androidx.compose.foundation.lazy.LazyListState] (#142). */
private sealed interface ClassCardRow {
    data class DayHeaderRow(val dayOfWeek: Int, val isToday: Boolean, val isNextClassDay: Boolean) : ClassCardRow
    data class ClassEntryRow(val entry: WeeklyClassEntry, val isOngoing: Boolean) : ClassCardRow
    data object EmptyRow : ClassCardRow
}

/** Filas planas de la tarjeta de "Próximas tareas", agrupadas por qué tan cerca está el
 *  vencimiento (#161) en vez de una sola lista plana — mismo patrón que [ClassCardRow]. */
private sealed interface TaskCardRow {
    data class BucketHeaderRow(val bucket: TaskUrgencyBucket) : TaskCardRow
    data class TaskEntryRow(val task: TaskEntity) : TaskCardRow
    data object EmptyRow : TaskCardRow
}

@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    onOpenSubject: (folderId: Long, subjectId: Long) -> Unit = { _, _ -> },
    onOpenTask: (folderId: Long, taskId: Long) -> Unit = { _, _ -> },
    /** Se incrementa cada vez que se toca el encabezado del widget de "Próximas clases" (#142): un
     *  nuevo valor (aunque ya se esté en Inicio) vuelve a disparar el scroll a la clase actual/próxima. */
    scrollToNextClassSignal: Int = 0,
    // Acciones rápidas (rediseño): alarma nueva general, saltar a Enfoque en Reloj, o ir a elegir
    // carpeta para una tarea nueva.
    onNewAlarm: () -> Unit = {},
    onOpenFocus: () -> Unit = {},
    onOpenNewTask: () -> Unit = {},
    onOpenWellbeing: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val weeklyClassDays by viewModel.weeklyClassDays.collectAsState()
    val upcomingTasks by viewModel.upcomingTasks.collectAsState()
    val subjectColorsById by viewModel.subjectColorsById.collectAsState()
    val subjectNamesById by viewModel.subjectNamesById.collectAsState()
    val subjectIconsById by viewModel.subjectIconsById.collectAsState()
    val todayScreenTimeMinutes by viewModel.todayScreenTimeMinutes.collectAsState()
    val nextClassOccurrence by viewModel.nextClassOccurrence.collectAsState()

    // Chequeo en vivo (no cacheado en el ViewModel) cada vez que se abre/vuelve a Inicio: si el
    // volumen de alarma está por debajo de la mitad, un aviso bien visible en vez de descubrirlo
    // recién cuando una alarma no suena lo bastante fuerte (#2).
    val context = LocalContext.current
    val isAlarmVolumeLow = remember { AlarmVolumeStatus.isLow(context) }

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
    // Igual que classCardRows: se agrupa en base al mismo "now" fijo de arriba, no hace falta que
    // se reclasifiquen en vivo mientras se mira la pantalla. Como upcomingTasks ya viene ordenada
    // por dueAtEpochMillis ascendente (con nulas al final), agrupar por bucket produce grupos ya
    // contiguos en ese mismo orden (vencidas → próximos días → esta semana → más adelante → sin fecha).
    val taskCardRows = remember(upcomingTasks, now) {
        if (upcomingTasks.isEmpty()) {
            listOf(TaskCardRow.EmptyRow)
        } else {
            val nowEpochMillis = now.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
            upcomingTasks
                .groupBy { taskUrgencyBucket(it.dueAtEpochMillis, nowEpochMillis) }
                .flatMap { (bucket, tasks) -> listOf(TaskCardRow.BucketHeaderRow(bucket)) + tasks.map { TaskCardRow.TaskEntryRow(it) } }
        }
    }
    val listState = rememberLazyListState()
    // +1 fila por el título "Próximas clases" (índice 0), que también es un item plano de la lista.
    val scrollTargetIndex = remember(classCardRows, todayDayOfWeek, nowMinuteOfDay) {
        findScrollTargetIndex(classCardRows, todayDayOfWeek, nowMinuteOfDay)?.plus(1)
    }
    // Al entrar a Inicio de CUALQUIER forma (abrir la app, tocar el tab, volver de otra pantalla),
    // arranca ya scrolleado al momento actual de la semana — sin animación (scrollToItem, no
    // animateScrollToItem): se siente como que la lista "ya estaba ahí", no como un salto.
    LaunchedEffect(Unit) {
        scrollTargetIndex?.let { listState.scrollToItem(it) }
    }
    // El toque en el encabezado del widget, en cambio, sí anima el scroll (más notorio, es una
    // acción explícita del usuario) y se repite aunque ya se esté en Inicio (ver el contador en
    // WakeUpNavHost).
    LaunchedEffect(scrollToNextClassSignal) {
        if (scrollToNextClassSignal > 0 && scrollTargetIndex != null) {
            listState.animateScrollToItem(scrollTargetIndex)
        }
    }

    Scaffold(
        topBar = {
            WakeUpTopBar(
                title = { Text("Wake up") },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Ajustes")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (isAlarmVolumeLow) {
                LowAlarmVolumeBanner(
                    onFix = {
                        PermissionIntents.safeStart(context, android.content.Intent(android.provider.Settings.ACTION_SOUND_SETTINGS))
                    },
                )
            }
            // Tarjeta destacada del mockup del rediseño: la clase en curso o la próxima, con cuenta
            // regresiva — fija arriba (no dentro del LazyColumn de abajo) para no tener que tocar el
            // cálculo de scrollTargetIndex de esa lista, que ya tiene varios casos límite resueltos
            // (#142, #143).
            nextClassOccurrence?.let { occurrence ->
                NextClassHeroCard(
                    occurrence = occurrence,
                    onClick = { onOpenSubject(occurrence.folderId, occurrence.subjectId) },
                    modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp),
                )
            }
            if (weeklyClassDays.isEmpty() && upcomingTasks.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
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
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), state = listState) {
                    item(key = "classes_title") {
                        CardTitleRow("Próximas clases", MaterialTheme.colorScheme.primary, roundedBottom = classCardRows.size <= 1 && classCardRows.firstOrNull() == ClassCardRow.EmptyRow)
                    }
                    itemsIndexed(classCardRows, key = { index, _ -> "class_row_$index" }) { index, row ->
                        val isLast = index == classCardRows.lastIndex
                        when (row) {
                            is ClassCardRow.EmptyRow -> CardEmptyText("No hay clases esta semana", roundedBottom = true, modifier = Modifier.animateItem())
                            is ClassCardRow.DayHeaderRow -> CardRowBackground(roundedBottom = false, modifier = Modifier.animateItem()) {
                                DayHeader(DIA_LARGO[row.dayOfWeek - 1], row.isToday, row.isNextClassDay)
                            }
                            is ClassCardRow.ClassEntryRow -> CardRowBackground(roundedBottom = isLast, bottomExtraPadding = isLast, modifier = Modifier.animateItem()) {
                                ClassRow(row.entry, row.isOngoing, onClick = { onOpenSubject(row.entry.folderId, row.entry.subjectId) })
                            }
                        }
                    }
                    item(key = "tasks_title") {
                        CardTitleRow(
                            "Próximas tareas",
                            MaterialTheme.colorScheme.secondary,
                            roundedBottom = taskCardRows.size <= 1 && taskCardRows.firstOrNull() == TaskCardRow.EmptyRow,
                            topPadding = 16.dp,
                        )
                    }
                    itemsIndexed(
                        taskCardRows,
                        key = { index, row ->
                            when (row) {
                                is TaskCardRow.EmptyRow -> "tasks_empty"
                                is TaskCardRow.BucketHeaderRow -> "task_bucket_${row.bucket}"
                                is TaskCardRow.TaskEntryRow -> "task_row_${row.task.id}"
                            }
                        },
                    ) { index, row ->
                        val isLast = index == taskCardRows.lastIndex
                        when (row) {
                            is TaskCardRow.EmptyRow -> CardEmptyText("No hay tareas próximas", roundedBottom = true, modifier = Modifier.animateItem())
                            is TaskCardRow.BucketHeaderRow -> CardRowBackground(roundedBottom = false, modifier = Modifier.animateItem()) {
                                TaskBucketHeader(row.bucket)
                            }
                            is TaskCardRow.TaskEntryRow -> CardRowBackground(roundedBottom = isLast, bottomExtraPadding = isLast, modifier = Modifier.animateItem()) {
                                TaskRow(
                                    row.task,
                                    subjectColorsById[row.task.subjectId],
                                    row.task.subjectId?.let { subjectIconsById[it] },
                                    row.task.subjectId?.let { subjectNamesById[it] },
                                    onClick = { onOpenTask(row.task.folderId, row.task.id) },
                                )
                            }
                        }
                    }
                    item(key = "quick_actions_title") {
                        Text(
                            "Acciones rápidas",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 20.dp, bottom = 10.dp),
                        )
                    }
                    item(key = "quick_actions") {
                        QuickActionsRow(onNewAlarm = onNewAlarm, onOpenFocus = onOpenFocus, onOpenNewTask = onOpenNewTask)
                    }
                    item(key = "wellbeing") {
                        WellbeingCard(
                            screenTimeMinutesToday = todayScreenTimeMinutes,
                            onClick = onOpenWellbeing,
                            modifier = Modifier.padding(top = 12.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Tarjeta destacada de la clase en curso/próxima (mockup del rediseño): píldora de estado y cuenta
 *  regresiva que se refresca sola cada 30s mientras esta pantalla está en pantalla — igual que
 *  "Faltan Xh Ym" en la lista de alarmas (mismo intervalo, mismo motivo: no hace falta cada segundo
 *  para una cuenta en minutos). */
@Composable
private fun NextClassHeroCard(occurrence: UpcomingClassOccurrence, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val use24Hour = LocalUse24HourFormat.current
    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(30_000)
            now = Clock.System.now()
        }
    }
    val zone = remember { TimeZone.currentSystemDefault() }
    val startInstant = remember(occurrence) { occurrence.start.toInstant(zone) }
    val endInstant = remember(occurrence) { occurrence.end.toInstant(zone) }
    val isOngoing = now >= startInstant && now < endInstant
    val countdownLabel = if (isOngoing) "Termina en" else "Empieza en"
    val countdown = AlarmTiming.formatRemaining(if (isOngoing) endInstant - now else startInstant - now)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                    .padding(horizontal = 10.dp, vertical = 5.dp),
            ) {
                Text(
                    if (isOngoing) "EN CURSO" else "PRÓXIMA CLASE",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
            }
            val startText = formatMinuteOfDay(occurrence.start.hour * 60 + occurrence.start.minute, use24Hour)
            val endText = formatMinuteOfDay(occurrence.end.hour * 60 + occurrence.end.minute, use24Hour)
            Text("$startText – $endText", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.outline)
        }
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 12.dp)) {
            // Mismo avatar circular (ícono o punto de color) que el resto de la app (#161) — antes
            // esta tarjeta no mostraba ninguna referencia visual a la materia, solo su nombre.
            SubjectIndicator(Color(occurrence.colorArgb), occurrence.iconKey, size = 36.dp)
            Column(modifier = Modifier.padding(start = 12.dp)) {
                Text(occurrence.subjectName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                if (occurrence.room.isNotBlank()) {
                    Text(occurrence.room, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
        Text(countdown, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 14.dp))
        Text(countdownLabel.uppercase(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.outline)
    }
}

/** Fila de 3 atajos (rediseño): crear una alarma general, saltar a Enfoque en Reloj, o ir a elegir
 *  la carpeta donde crear una tarea. Un color de acento distinto por ícono, como en el mockup. */
@Composable
private fun QuickActionsRow(onNewAlarm: () -> Unit, onOpenFocus: () -> Unit, onOpenNewTask: () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
        QuickActionButton("Alarma", Icons.Default.Alarm, MaterialTheme.colorScheme.secondary, onNewAlarm, Modifier.weight(1f))
        QuickActionButton("Enfoque", Icons.Default.Timer, MaterialTheme.colorScheme.primary, onOpenFocus, Modifier.weight(1f))
        QuickActionButton("Tarea", Icons.Default.AddTask, MaterialTheme.colorScheme.tertiary, onOpenNewTask, Modifier.weight(1f))
    }
}

@Composable
private fun QuickActionButton(label: String, icon: ImageVector, accent: Color, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** Resumen de pantalla de hoy (rediseño), con el mismo dato que ya muestra la pestaña Bienestar —
 *  toca para ir ahí. Sin porcentaje de "límite diario": la app no tiene un límite general de
 *  pantalla configurable (el límite que sí existe es específico de Reels/TikTok, ver Bloqueo), así
 *  que mostrar un porcentaje inventado sería engañoso. */
@Composable
private fun WellbeingCard(screenTimeMinutesToday: Long, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Default.Favorite, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.padding(start = 14.dp)) {
            val hours = screenTimeMinutesToday / 60
            val minutes = screenTimeMinutesToday % 60
            val label = if (hours > 0) "${hours}h ${minutes}m de pantalla hoy" else "${minutes}m de pantalla hoy"
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                "Toca para ver el detalle",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
            )
        }
    }
}

@Composable
private fun LowAlarmVolumeBanner(onFix: () -> Unit) {
    val onWarning = MaterialTheme.colorScheme.error
    Card(
        modifier = Modifier.fillMaxWidth().padding(16.dp, 16.dp, 16.dp, 0.dp),
        colors = CardDefaults.cardColors(containerColor = onWarning.copy(alpha = 0.10f)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.VolumeOff, contentDescription = null, tint = onWarning)
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text("Volumen de alarma bajo", style = MaterialTheme.typography.titleSmall, color = onWarning)
                Text(
                    "Está por debajo de la mitad: puede que no te despiertes con tus alarmas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            TextButton(onClick = onFix) { Text("Subir", color = onWarning, fontWeight = FontWeight.Bold) }
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
private fun CardEmptyText(text: String, roundedBottom: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
private fun CardRowBackground(roundedBottom: Boolean, bottomExtraPadding: Boolean = false, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
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
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f))
                .padding(horizontal = 10.dp, vertical = 3.dp),
        ) {
            Text("$label · Hoy", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
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

/** Encabezado de grupo dentro de "Próximas tareas" (#161) — mismo estilo discreto que la etiqueta
 *  de día "normal" de [DayHeader], salvo "Atrasadas", que se resalta en rojo porque de verdad
 *  necesita atención. */
@Composable
private fun TaskBucketHeader(bucket: TaskUrgencyBucket) {
    val label = when (bucket) {
        TaskUrgencyBucket.OVERDUE -> "Atrasadas"
        TaskUrgencyBucket.WITHIN_3_DAYS -> "Próximos días"
        TaskUrgencyBucket.WITHIN_1_WEEK -> "Esta semana"
        TaskUrgencyBucket.LATER -> "Más adelante"
        TaskUrgencyBucket.NO_DUE_DATE -> "Sin fecha"
    }
    val isOverdue = bucket == TaskUrgencyBucket.OVERDUE
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (isOverdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
        fontWeight = if (isOverdue) FontWeight.Bold else FontWeight.Normal,
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 2.dp),
    )
}

/** [iconKey] no nulo (materia con ícono elegido) reemplaza la barra fina de color por el mismo
 *  avatar circular con ícono que ya usa la lista de materias de una carpeta (#161). */
@Composable
private fun ItemRow(accentColor: Color, iconKey: String? = null, highlighted: Boolean = false, onClick: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (com.fritangui.wakeup.ui.subjects.SubjectIcons.iconFor(iconKey) != null) {
            SubjectIndicator(accentColor, iconKey, size = 40.dp)
        } else {
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(40.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(accentColor),
            )
        }
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
    ItemRow(accentColor = Color(entry.colorArgb), iconKey = entry.iconKey, highlighted = isOngoing, onClick = onClick) {
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
private fun TaskRow(task: TaskEntity, subjectColorArgb: Int?, subjectIconKey: String?, subjectName: String?, onClick: () -> Unit) {
    val use24Hour = LocalUse24HourFormat.current
    ItemRow(
        accentColor = subjectColorArgb?.let { Color(it) } ?: MaterialTheme.colorScheme.secondary,
        iconKey = subjectIconKey,
        onClick = onClick,
    ) {
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
