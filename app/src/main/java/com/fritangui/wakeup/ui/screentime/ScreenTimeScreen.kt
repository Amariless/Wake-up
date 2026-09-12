@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.screentime

import android.content.Context
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import com.fritangui.wakeup.ui.components.WakeUpTopBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus
import kotlinx.datetime.LocalDate

/** Las dos formas de mirar el historial de uso, ver #161. */
private enum class ScreenTimeRangeMode { WEEK, MONTH }

private val MESES_CORTOS = listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
private val MESES_LARGOS = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre",
)

/** "Esta semana" si es la actual; si no, el rango de fechas ("1 sep – 7 sep"). */
private fun weekRangeLabel(weekOffset: Int, days: List<DayUsage>): String {
    if (weekOffset == 0) return "Esta semana"
    val first = days.firstOrNull() ?: return ""
    val last = days.lastOrNull() ?: return ""
    val firstDate = LocalDate.fromEpochDays(first.epochDay.toInt())
    val lastDate = LocalDate.fromEpochDays(last.epochDay.toInt())
    return "${firstDate.dayOfMonth} ${MESES_CORTOS[firstDate.monthNumber - 1]} – ${lastDate.dayOfMonth} ${MESES_CORTOS[lastDate.monthNumber - 1]}"
}

/** "Septiembre 2026" — el mes se deduce del primer día del rango recibido. */
private fun monthRangeLabel(days: List<DayUsage>): String {
    val first = days.firstOrNull() ?: return ""
    val date = LocalDate.fromEpochDays(first.epochDay.toInt())
    return "${MESES_LARGOS[date.monthNumber - 1]} ${date.year}"
}

@Composable
fun ScreenTimeScreen(onOpenBlocking: () -> Unit, viewModel: ScreenTimeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val usage by viewModel.todayUsage.collectAsState()
    val yesterdayTotal by viewModel.yesterdayTotalMinutes.collectAsState()
    val weekly by viewModel.weeklyUsage.collectAsState()
    val monthly by viewModel.monthlyUsage.collectAsState()
    val weekOffset by viewModel.weekOffset.collectAsState()
    val monthOffset by viewModel.monthOffset.collectAsState()
    val canGoToPreviousWeek by viewModel.canGoToPreviousWeek.collectAsState()
    val canGoToPreviousMonth by viewModel.canGoToPreviousMonth.collectAsState()
    val rules by viewModel.alertRules.collectAsState()
    val hasUsageAccess = remember { PermissionStatus.hasUsageAccess(context) }

    // "Hoy" arriba de todo siempre es HOY, sin que le afecte navegar semanas/meses más abajo (#161).
    var rangeMode by remember { mutableStateOf(ScreenTimeRangeMode.WEEK) }
    val todayTotal = usage.sumOf { it.minutes }
    val activeDays = if (rangeMode == ScreenTimeRangeMode.WEEK) weekly else monthly
    val activeAverage = if (activeDays.isNotEmpty()) activeDays.sumOf { it.totalMinutes } / activeDays.size else 0L

    Scaffold(topBar = { WakeUpTopBar(title = { Text("Tiempo de pantalla") }) }) { padding ->
        // Sin scroll el contenido (permiso, hoy, semana+gráfico, hasta 10 apps, avisos, y el botón
        // de bloqueo) podía no caber en pantallas más chicas — el botón de abajo quedaba cortado o
        // pegado sin aire, dando la sensación de que "no pertenecía a nada".
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            if (!hasUsageAccess) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Falta el permiso de acceso a datos de uso", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sin él, Wake up no puede medir cuánto usas cada app.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        TextButton(onClick = { PermissionIntents.safeStart(context, PermissionIntents.usageAccessSettings()) }) {
                            Text("Activar ahora")
                        }
                    }
                }
            }

            // Resumen del día arriba de todo: es el número que más importa de un vistazo.
            Text("Hoy", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.outline)
            Text(formatDuration(todayTotal), style = MaterialTheme.typography.displaySmall)
            run {
                val diff = todayTotal - yesterdayTotal
                Text(
                    when {
                        diff == 0L -> "Igual que ayer"
                        diff > 0 -> "+${formatDuration(diff)} más que ayer"
                        else -> "-${formatDuration(-diff)} menos que ayer"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            // Antes esto era fijo ("Últimos 7 días", siempre los mismos): ahora se puede navegar a
            // semanas pasadas o cambiar a una vista mes a mes (#161) — sin límite de cuánto atrás
            // (los datos existen desde que se instaló la app), pero sin poder ir al futuro.
            Text("Historial", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = rangeMode == ScreenTimeRangeMode.WEEK,
                    onClick = { rangeMode = ScreenTimeRangeMode.WEEK },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                ) { Text("Semana") }
                SegmentedButton(
                    selected = rangeMode == ScreenTimeRangeMode.MONTH,
                    onClick = { rangeMode = ScreenTimeRangeMode.MONTH },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                ) { Text("Mes") }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // No deja ir a un período de antes de que hubiera cualquier dato registrado (#161):
                // sin esto, se podía navegar indefinidamente hacia atrás a semanas/meses siempre
                // vacíos, sin ninguna señal de que ahí ya no hay nada más que ver.
                val canGoPrevious = if (rangeMode == ScreenTimeRangeMode.WEEK) canGoToPreviousWeek else canGoToPreviousMonth
                IconButton(
                    onClick = { if (rangeMode == ScreenTimeRangeMode.WEEK) viewModel.previousWeek() else viewModel.previousMonth() },
                    enabled = canGoPrevious,
                ) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = if (rangeMode == ScreenTimeRangeMode.WEEK) "Semana anterior" else "Mes anterior")
                }
                Text(
                    if (rangeMode == ScreenTimeRangeMode.WEEK) weekRangeLabel(weekOffset, weekly) else monthRangeLabel(monthly),
                    style = MaterialTheme.typography.titleMedium,
                )
                val atPresent = if (rangeMode == ScreenTimeRangeMode.WEEK) weekOffset == 0 else monthOffset == 0
                // Sin tint manual: IconButton ya atenúa su ícono solo cuando enabled = false.
                IconButton(
                    onClick = { if (rangeMode == ScreenTimeRangeMode.WEEK) viewModel.nextWeek() else viewModel.nextMonth() },
                    enabled = !atPresent,
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = if (rangeMode == ScreenTimeRangeMode.WEEK) "Semana siguiente" else "Mes siguiente",
                    )
                }
            }
            // Crossfade + animateContentSize (en vez de un if/else liso): cambiar de semana/mes, o
            // de vista Semana↔Mes, ya no es un salto seco — la sección se desvanece y el alto se
            // acomoda solo entre "hay datos" (gráfico) y "sin datos" (#161).
            Box(modifier = Modifier.fillMaxWidth().animateContentSize()) {
                Crossfade(targetState = rangeMode to activeDays.any { it.totalMinutes > 0 }, label = "screen_time_history") { (mode, hasData) ->
                    if (hasData) {
                        Column {
                            Text(
                                "Promedio diario: ${formatDuration(activeAverage)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.outline,
                                modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
                            )
                            if (mode == ScreenTimeRangeMode.WEEK) WeeklyBarChart(weekly) else MonthlyBarChart(monthly)
                        }
                    } else {
                        // El período ACTUAL sin nada más que hoy (uso recién empezando a medirse,
                        // o recién instalada la app) es distinto de navegar a un período pasado que
                        // de verdad no tiene nada — en ese caso el mensaje genérico no aporta, mejor
                        // decir derecho que por ahora solo hay datos de hoy (#161).
                        val isCurrentPeriod = if (mode == ScreenTimeRangeMode.WEEK) weekOffset == 0 else monthOffset == 0
                        Text(
                            if (isCurrentPeriod && todayTotal > 0) {
                                "Todavía no hay suficiente historial — por ahora solo hay datos de hoy"
                            } else {
                                "Sin datos de uso en este período"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(top = 12.dp, bottom = 12.dp),
                        )
                    }
                }
            }

            Text("Por app hoy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            if (usage.isEmpty()) {
                Text("Aún no hay datos suficientes (se actualiza cada ~15 min)")
            } else {
                val maxMinutes = (usage.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1L)
                usage.take(10).forEach { row ->
                    UsageBarRow(packageName = row.packageName, label = row.label, minutes = row.minutes, maxMinutes = maxMinutes)
                }
            }

            Text("Avisos", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            if (rules.isEmpty()) {
                TextButton(onClick = { viewModel.addSocialMediaRule(90) }) {
                    Text("+ Avisarme tras 90 min/día en redes sociales")
                }
            } else {
                rules.forEach { rule ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${rule.label} · ${rule.dailyThresholdMinutes} min/día")
                        Switch(checked = rule.isEnabled, onCheckedChange = { viewModel.setRuleEnabled(rule, it) })
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 24.dp))
            TextButton(onClick = onOpenBlocking, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
                Text("Configurar bloqueo de Reels/TikTok →")
            }
        }
    }
}

@Composable
private fun WeeklyBarChart(days: List<DayUsage>) {
    val maxMinutes = (days.maxOfOrNull { it.totalMinutes } ?: 1L).coerceAtLeast(1L)
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { day ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                // El Canvas de la barra no expone nada a servicios de accesibilidad por sí solo, y
                // antes solo el nombre corto del día ("Lu") era legible por TalkBack, sin el dato de
                // minutos que es la información central del gráfico.
                modifier = Modifier.weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = "${day.dayLabelFull}: ${formatDuration(day.totalMinutes)}" },
            ) {
                val barColor = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                Canvas(modifier = Modifier.weight(1f).width(20.dp)) {
                    val fraction = (day.totalMinutes.toFloat() / maxMinutes).coerceIn(if (day.totalMinutes > 0) 0.04f else 0f, 1f)
                    val barHeight = size.height * fraction
                    drawRect(
                        color = barColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                        size = Size(size.width, barHeight),
                    )
                }
                Text(
                    day.dayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (day.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

/** Mismo gráfico que [WeeklyBarChart] pero para hasta 31 barras (#161): sin nombre de día bajo
 *  cada una (no entrarían), solo el número de día en el 1°, el último y cada 5 — la fecha completa
 *  de cada barra sigue disponible para TalkBack vía sus semantics. */
@Composable
private fun MonthlyBarChart(days: List<DayUsage>) {
    val maxMinutes = (days.maxOfOrNull { it.totalMinutes } ?: 1L).coerceAtLeast(1L)
    val lastDayOfMonth = days.size
    Row(
        modifier = Modifier.fillMaxWidth().height(120.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        days.forEach { day ->
            val dayOfMonth = LocalDate.fromEpochDays(day.epochDay.toInt()).dayOfMonth
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
                    .semantics(mergeDescendants = true) { contentDescription = "${day.dayLabelFull} $dayOfMonth: ${formatDuration(day.totalMinutes)}" },
            ) {
                val barColor = if (day.isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                Canvas(modifier = Modifier.weight(1f).width(6.dp)) {
                    val fraction = (day.totalMinutes.toFloat() / maxMinutes).coerceIn(if (day.totalMinutes > 0) 0.04f else 0f, 1f)
                    val barHeight = size.height * fraction
                    drawRect(
                        color = barColor,
                        topLeft = androidx.compose.ui.geometry.Offset(0f, size.height - barHeight),
                        size = Size(size.width, barHeight),
                    )
                }
                if (dayOfMonth == 1 || dayOfMonth == lastDayOfMonth || dayOfMonth % 5 == 0) {
                    Text(
                        "$dayOfMonth",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (day.isToday) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                } else {
                    // Mismo alto que la rama con número, para que todas las barras midan igual sin
                    // que las que no tienen etiqueta abajo "salten" un poco más alto que las que sí.
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun UsageBarRow(packageName: String, label: String, minutes: Long, maxMinutes: Long) {
    val context = LocalContext.current
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        AppIconSmall(context, packageName, label)
        Text(
            label,
            modifier = Modifier.width(90.dp).padding(start = 10.dp),
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
        )
        val trackColor = MaterialTheme.colorScheme.surfaceVariant
        val fillColor = MaterialTheme.colorScheme.primary
        Canvas(modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 8.dp)) {
            val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0.02f, 1f)
            drawRect(color = trackColor, size = size)
            drawRect(color = fillColor, size = Size(size.width * fraction, size.height))
        }
        Text(formatDuration(minutes), style = MaterialTheme.typography.bodyMedium)
    }
}

/** Ícono real de la app (#152) — cae a un ícono genérico si no se puede leer (p.ej. se desinstaló). */
@Composable
private fun AppIconSmall(context: Context, packageName: String, label: String) {
    val bitmap = remember(packageName) {
        runCatching { context.packageManager.getApplicationIcon(packageName).toBitmap().asImageBitmap() }.getOrNull()
    }
    Box(
        modifier = Modifier.size(24.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = label, modifier = Modifier.size(18.dp))
        } else {
            Icon(Icons.Default.Apps, contentDescription = label, modifier = Modifier.size(14.dp))
        }
    }
}

/** "<1m" para uso real pero menor a un minuto (antes decía "0m", que se leía como "nada de uso" en
 *  vez de "casi nada"), "45m" si es menos de una hora, "1h 23m" (o "2h" sin minutos sueltos) si es
 *  una hora o más. */
private fun formatDuration(minutes: Long): String {
    if (minutes <= 0) return "<1m"
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}
