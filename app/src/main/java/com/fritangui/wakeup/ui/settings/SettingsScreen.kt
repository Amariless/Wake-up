@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.BuildConfig

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenXiaomiWizard: () -> Unit,
    onOpenDevTools: () -> Unit,
    onOpenUpdate: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val dynamicColor by viewModel.dynamicColorEnabled.collectAsState()
    val use24HourFormat by viewModel.use24HourFormat.collectAsState()
    val snoozeMinutes by viewModel.snoozeMinutes.collectAsState()
    val blockGraceMinutes by viewModel.blockGraceMinutes.collectAsState()
    val nextClassNotificationMinutes by viewModel.nextClassNotificationMinutes.collectAsState()
    val lowAlarmVolumeWarningEnabled by viewModel.lowAlarmVolumeWarningEnabled.collectAsState()
    val lowAlarmVolumeWarningHoursAhead by viewModel.lowAlarmVolumeWarningHoursAhead.collectAsState()

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
        // Sin verticalScroll el contenido se cortaba abajo (más visible con "Color dinámico" +
        // "Buscar actualizaciones" + "Permisos" + panel de dev en builds debug, ver #148).
        Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SettingsRow("Color dinámico", "Usa los colores del fondo de tu teléfono (Android 12+)") {
                Switch(checked = dynamicColor, onCheckedChange = viewModel::setDynamicColorEnabled)
            }
            HorizontalDivider()
            SettingsRow("Formato de 24 horas", "Si está apagado, se muestra la hora en 12h con AM/PM") {
                Switch(checked = use24HourFormat, onCheckedChange = viewModel::setUse24HourFormat)
            }
            HorizontalDivider()
            SettingsRow("Posponer alarma", "Minutos que espera el botón \"Posponer\" al sonar una alarma") {
                NumberStepper(value = snoozeMinutes, range = 1..30, onValueChange = viewModel::setSnoozeMinutes)
            }
            HorizontalDivider()
            SettingsRow("Prórroga de bloqueo", "Minutos del botón \"X minutos más\" al alcanzar el límite de Reels/TikTok") {
                NumberStepper(value = blockGraceMinutes, range = 1..30, onValueChange = viewModel::setBlockGraceMinutes)
            }
            HorizontalDivider()
            SettingsRow("Aviso de próxima clase", "Notificación cuando tu próxima clase está por empezar") {
                Switch(checked = nextClassNotificationMinutes > 0, onCheckedChange = viewModel::setNextClassNotificationEnabled)
            }
            if (nextClassNotificationMinutes > 0) {
                SettingsRow("Minutos de anticipación", "Con cuánto tiempo antes avisar") {
                    NumberStepper(value = nextClassNotificationMinutes, range = 1..60, onValueChange = viewModel::setNextClassNotificationMinutes)
                }
            }
            HorizontalDivider()
            SettingsRow("Aviso de volumen bajo", "Avisa si el volumen de alarma está por debajo de la mitad y tienes una alarma por sonar pronto") {
                Switch(checked = lowAlarmVolumeWarningEnabled, onCheckedChange = viewModel::setLowAlarmVolumeWarningEnabled)
            }
            if (lowAlarmVolumeWarningEnabled) {
                SettingsRow("Con cuánta anticipación", "Horas antes de la alarma para empezar a avisar") {
                    NumberStepper(value = lowAlarmVolumeWarningHoursAhead, range = 1..24, onValueChange = viewModel::setLowAlarmVolumeWarningHoursAhead)
                }
            }
            HorizontalDivider()
            SettingsLinkRow("Buscar actualizaciones", "Revisa el repositorio de GitHub por una versión nueva", onOpenUpdate)
            SettingsLinkRow("Permisos", "Notificaciones, alarmas exactas, accesibilidad y más", onOpenXiaomiWizard)
            // Tiempo de pantalla y Bloqueo de Reels/TikTok ya no están acá: la pestaña Bienestar
            // (antes "Tiempo de pantalla") ya cubre las dos, tenerlas duplicadas en Ajustes solo
            // confundía — y de paso, navegar hasta Bienestar por acá dejaba una entrada extra en la
            // pila de navegación que a veces hacía que el tab de Inicio terminara cayendo ahí en
            // vez de a Inicio.
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
private fun NumberStepper(value: Int, range: IntRange, onValueChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { if (value - 1 in range) onValueChange(value - 1) }, enabled = value - 1 in range) {
            Icon(Icons.Default.Remove, contentDescription = "Menos")
        }
        Text(
            "$value",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(28.dp),
        )
        IconButton(onClick = { if (value + 1 in range) onValueChange(value + 1) }, enabled = value + 1 in range) {
            Icon(Icons.Default.Add, contentDescription = "Más")
        }
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
