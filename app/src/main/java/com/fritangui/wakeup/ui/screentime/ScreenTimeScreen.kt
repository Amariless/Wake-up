@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.screentime

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus
import com.fritangui.wakeup.ui.theme.WakeUpPrimary
import com.fritangui.wakeup.ui.theme.WakeUpSecondary

@Composable
fun ScreenTimeScreen(onOpenBlocking: () -> Unit, viewModel: ScreenTimeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val usage by viewModel.todayUsage.collectAsState()
    val weekly by viewModel.weeklyUsage.collectAsState()
    val rules by viewModel.alertRules.collectAsState()
    val hasUsageAccess = remember { PermissionStatus.hasUsageAccess(context) }

    val todayTotal = usage.sumOf { it.minutes }
    // El día de ayer siempre es el penúltimo de los 7 (el último es hoy) — ver ScreenTimeViewModel.weeklyUsage.
    val yesterdayTotal = weekly.getOrNull(weekly.size - 2)?.totalMinutes
    val weeklyAverage = if (weekly.isNotEmpty()) weekly.sumOf { it.totalMinutes } / weekly.size else 0L

    Scaffold(topBar = { TopAppBar(title = { Text("Tiempo de pantalla") }) }) { padding ->
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
            if (yesterdayTotal != null) {
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

            if (weekly.any { it.totalMinutes > 0 }) {
                Text("Últimos 7 días", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 4.dp))
                Text(
                    "Promedio diario: ${formatDuration(weeklyAverage)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
                WeeklyBarChart(weekly)
            }

            Text("Por app hoy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            if (usage.isEmpty()) {
                Text("Aún no hay datos suficientes (se actualiza cada ~15 min)")
            } else {
                val maxMinutes = (usage.maxOfOrNull { it.minutes } ?: 1L).coerceAtLeast(1L)
                usage.take(10).forEach { row ->
                    UsageBarRow(label = row.label, minutes = row.minutes, maxMinutes = maxMinutes)
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
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                val barColor = if (day.isToday) WakeUpPrimary else WakeUpSecondary
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

@Composable
private fun UsageBarRow(label: String, minutes: Long, maxMinutes: Long) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(110.dp), style = MaterialTheme.typography.bodyMedium, maxLines = 1)
        Canvas(modifier = Modifier.weight(1f).height(18.dp).padding(horizontal = 8.dp)) {
            val fraction = (minutes.toFloat() / maxMinutes).coerceIn(0.02f, 1f)
            drawRect(color = Color(0x22FFFFFF), size = size)
            drawRect(color = WakeUpPrimary, size = Size(size.width * fraction, size.height))
        }
        Text(formatDuration(minutes), style = MaterialTheme.typography.bodyMedium)
    }
}

/** "45m" si es menos de una hora, "1h 23m" (o "2h" sin minutos sueltos) si es una hora o más. */
private fun formatDuration(minutes: Long): String {
    if (minutes < 60) return "${minutes}m"
    val h = minutes / 60
    val m = minutes % 60
    return if (m == 0L) "${h}h" else "${h}h ${m}m"
}
