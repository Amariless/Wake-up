@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.BuildConfig

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenXiaomiWizard: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenBlocking: () -> Unit,
    onOpenDevTools: () -> Unit,
    onOpenUpdate: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ajustes") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SettingsRow("Color dinámico", "Usa los colores del fondo de tu teléfono (Android 12+)") {
                Switch(checked = dynamicColor, onCheckedChange = viewModel::setDynamicColorEnabled)
            }
            HorizontalDivider()
            SettingsRow("Formato de 24 horas", "Si está apagado, se muestra la hora en 12h con AM/PM") {
                Switch(checked = use24HourFormat, onCheckedChange = viewModel::setUse24HourFormat)
            }
            HorizontalDivider()
            SettingsLinkRow("Buscar actualizaciones", "Revisa el repositorio de GitHub por una versión nueva", onOpenUpdate)
            SettingsLinkRow("Permisos", "Notificaciones, alarmas exactas, accesibilidad y más", onOpenXiaomiWizard)
            SettingsLinkRow("Tiempo de pantalla", "Cuánto usas cada app hoy", onOpenScreenTime)
            SettingsLinkRow("Bloqueo de Reels/TikTok", "Límites diarios de scroll infinito", onOpenBlocking)
            if (BuildConfig.DEV_TOOLS_ENABLED) {
                HorizontalDivider()
                SettingsLinkRow("Panel de desarrollador", "Solo en builds debug", onOpenDevTools)
            }
            HorizontalDivider()
            Text(
                "Wake up ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, trailing: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        trailing()
    }
}

@Composable
private fun SettingsLinkRow(title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.outline)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}
