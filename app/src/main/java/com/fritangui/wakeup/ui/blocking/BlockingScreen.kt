@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.blocking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus

private fun surfaceLabel(surface: BlockSurface) = when (surface) {
    BlockSurface.INSTAGRAM_REELS -> "Reels de Instagram"
    BlockSurface.TIKTOK_FOR_YOU -> "Feed \"Para ti\" de TikTok"
    BlockSurface.GENERIC_APP_TIME_LIMIT -> "App"
}

@Composable
fun BlockingScreen(onBack: () -> Unit, viewModel: BlockingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val rules by viewModel.rules.collectAsState()
    val usage by viewModel.todayUsageMinutes.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val accessibilityEnabled = remember { PermissionStatus.hasAccessibilityServiceEnabled(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueo de contenido") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) { Icon(Icons.Default.Add, contentDescription = "Nueva regla") }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (!accessibilityEnabled) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Falta activar el servicio de accesibilidad", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sin él, Wake up no puede detectar cuándo estás viendo Reels para poder limitarlo.",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
                        )
                        TextButton(onClick = { PermissionIntents.safeStart(context, PermissionIntents.accessibilitySettings()) }) {
                            Text("Activar ahora")
                        }
                    }
                }
            }

            Text(
                "El bloqueo nunca afecta los mensajes directos: solo actúa sobre el feed de scroll infinito.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (rules.isEmpty()) {
                Text("Toca + para crear tu primera regla, p.ej. \"30 min de Reels al día\"")
            } else {
                LazyColumn {
                    items(rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            usedMinutes = usage[rule.surface] ?: 0L,
                            onToggle = { viewModel.setEnabled(rule, it) },
                            onLimitChange = { viewModel.updateLimit(rule, it) },
                            onDelete = { viewModel.delete(rule) },
                        )
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddRuleDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { surface, minutes ->
                viewModel.addRule(surface, minutes)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun RuleRow(
    rule: BlockRuleEntity,
    usedMinutes: Long,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(surfaceLabel(rule.surface), style = MaterialTheme.typography.titleMedium)
                Switch(checked = rule.isEnabled, onCheckedChange = onToggle)
            }
            Text("Hoy: $usedMinutes / ${rule.dailyLimitMinutes} min", style = MaterialTheme.typography.bodyMedium)
            var sliderValue by remember(rule.id) { mutableIntStateOf(rule.dailyLimitMinutes) }
            Slider(
                value = sliderValue.toFloat(),
                onValueChange = { sliderValue = it.toInt() },
                onValueChangeFinished = { onLimitChange(sliderValue) },
                valueRange = 5f..180f,
            )
            TextButton(onClick = onDelete) { Text("Eliminar regla") }
        }
    }
}

@Composable
private fun AddRuleDialog(onDismiss: () -> Unit, onAdd: (BlockSurface, Int) -> Unit) {
    var surface by remember { mutableStateOf(BlockSurface.INSTAGRAM_REELS) }
    var minutesText by remember { mutableStateOf("30") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva regla de bloqueo") },
        text = {
            Column {
                listOf(BlockSurface.INSTAGRAM_REELS, BlockSurface.TIKTOK_FOR_YOU).forEach { option ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        androidx.compose.material3.RadioButton(selected = surface == option, onClick = { surface = option })
                        Text(surfaceLabel(option))
                    }
                }
                OutlinedTextField(
                    value = minutesText,
                    onValueChange = { minutesText = it.filter(Char::isDigit) },
                    label = { Text("Límite diario (minutos)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onAdd(surface, minutesText.toIntOrNull() ?: 30) }) { Text("Crear") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
