@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
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
    onOpenXiaomiWizard: () -> Unit,
    onOpenScreenTime: () -> Unit,
    onOpenBlocking: () -> Unit,
    onOpenDevTools: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Ajustes") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            SettingsRow("Color dinámico", "Usa los colores del fondo de tu teléfono (Android 12+)") {
                Switch(checked = dynamicColor, onCheckedChange = viewModel::setDynamicColorEnabled)
            }
            HorizontalDivider()
            SettingsLinkRow("Permisos (Xiaomi/MIUI y generales)", onOpenXiaomiWizard)
            SettingsLinkRow("Tiempo de pantalla", onOpenScreenTime)
            SettingsLinkRow("Bloqueo de Reels/TikTok", onOpenBlocking)
            if (BuildConfig.DEV_TOOLS_ENABLED) {
                HorizontalDivider()
                SettingsLinkRow("Panel de desarrollador", onOpenDevTools)
            }
            HorizontalDivider()
            Text(
                "Wake up ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.bodySmall,
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
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
        trailing()
    }
}

@Composable
private fun SettingsLinkRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
    }
}
