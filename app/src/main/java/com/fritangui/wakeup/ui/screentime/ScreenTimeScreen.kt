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
import androidx.compose.material3.Card
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

@Composable
fun ScreenTimeScreen(onOpenBlocking: () -> Unit, viewModel: ScreenTimeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val usage by viewModel.todayUsage.collectAsState()
    val rules by viewModel.alertRules.collectAsState()
    val hasUsageAccess = remember { PermissionStatus.hasUsageAccess(context) }

    Scaffold(topBar = { TopAppBar(title = { Text("Tiempo de pantalla") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
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

            Text("Hoy", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 8.dp))
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

            TextButton(onClick = onOpenBlocking, modifier = Modifier.padding(top = 16.dp)) {
                Text("Configurar bloqueo de Reels/TikTok →")
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
        Text("${minutes}m", style = MaterialTheme.typography.bodyMedium)
    }
}
