@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.blocking

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
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
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.blocking.BlockableApp
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus

@Composable
fun BlockingScreen(onBack: () -> Unit, viewModel: BlockingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val rows by viewModel.rows.collectAsState()
    val accessibilityEnabled = remember { PermissionStatus.hasAccessibilityServiceEnabled(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bloqueo de contenido") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            if (!accessibilityEnabled) {
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Falta activar el servicio de accesibilidad", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "Sin él, Wake up no puede detectar cuánto usas estas apps para poder limitarlas.",
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
                "Instagram y TikTok solo se limitan en su feed de scroll infinito (nunca en los mensajes directos); el resto se limita como app completa.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            LazyColumn {
                items(rows, key = { it.app.surface }) { row ->
                    BlockAppRow(
                        row = row,
                        onToggle = { viewModel.setEnabled(row, it) },
                        onLimitChange = { viewModel.updateLimit(row, it) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BlockAppRow(
    row: BlockingViewModel.Row,
    onToggle: (Boolean) -> Unit,
    onLimitChange: (Int) -> Unit,
) {
    val context = LocalContext.current
    val enabled = row.isInstalled && (row.rule?.isEnabled == true)
    val currentLimit = row.rule?.dailyLimitMinutes ?: row.app.defaultDailyLimitMinutes
    var minutesText by remember(row.app.surface, row.rule?.dailyLimitMinutes) { mutableStateOf(currentLimit.toString()) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.alpha(if (row.isInstalled) 1f else 0.4f)) {
                    AppIcon(context, row.app)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(row.app.label, style = MaterialTheme.typography.titleMedium)
                        if (!row.isInstalled) {
                            Text("No instalada", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Switch(checked = row.rule?.isEnabled == true, onCheckedChange = onToggle, enabled = row.isInstalled)
            }

            Column(modifier = Modifier.alpha(if (enabled) 1f else 0.4f).padding(top = 8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Hoy: ${row.usedMinutes} / ", style = MaterialTheme.typography.bodyMedium)
                    // Número editable a mano (#154): tocarlo y escribir el límite directo, en vez de
                    // depender solo del slider (impreciso para valores puntuales como "22 min").
                    OutlinedTextField(
                        value = minutesText,
                        onValueChange = { text ->
                            minutesText = text.filter(Char::isDigit).take(3)
                            minutesText.toIntOrNull()?.let(onLimitChange)
                        },
                        singleLine = true,
                        enabled = row.isInstalled,
                        textStyle = MaterialTheme.typography.bodyMedium,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.width(64.dp),
                    )
                    Text(" min", style = MaterialTheme.typography.bodyMedium)
                }
                Slider(
                    value = (minutesText.toIntOrNull() ?: currentLimit).toFloat(),
                    onValueChange = { minutesText = it.toInt().toString() },
                    onValueChangeFinished = { minutesText.toIntOrNull()?.let(onLimitChange) },
                    valueRange = 5f..180f,
                    enabled = row.isInstalled,
                )
            }
        }
    }
}

@Composable
private fun AppIcon(context: Context, app: BlockableApp) {
    val bitmap = remember(app.packageName) {
        runCatching { context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }.getOrNull()
    }
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = app.label, modifier = Modifier.size(28.dp))
        } else {
            Icon(Icons.Default.Apps, contentDescription = app.label, modifier = Modifier.size(22.dp))
        }
    }
}
