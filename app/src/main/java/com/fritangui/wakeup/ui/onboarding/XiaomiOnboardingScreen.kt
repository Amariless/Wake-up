@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.onboarding

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus

private data class ChecklistItem(
    val title: String,
    val description: String,
    val isXiaomiOnly: Boolean = false,
    val isChecked: () -> Boolean,
    val launch: (android.content.Context) -> Unit,
)

/**
 * Wizard de permisos: no hay API pública para forzar ninguno de estos ajustes,
 * así que se guía al usuario paso a paso y se le deja auto-verificar (los que
 * sí se pueden comprobar por código, como notificaciones o accesibilidad, se
 * marcan solos al volver a la pantalla).
 */
@Composable
fun XiaomiOnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    val isXiaomi = remember { PermissionStatus.isXiaomiDevice() }
    var refreshTick by remember { mutableStateOf(0) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshTick++ }

    val items = remember(refreshTick) {
        buildList {
            add(
                ChecklistItem(
                    "Notificaciones",
                    "Necesarias para avisos de alarma, tareas y tiempo de pantalla.",
                    isChecked = { PermissionStatus.hasNotificationPermission(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.notificationSettings(it)) },
                ),
            )
            add(
                ChecklistItem(
                    "Alarmas y recordatorios exactos",
                    "Para que las alarmas suenen exactamente a la hora programada.",
                    isChecked = { PermissionStatus.hasExactAlarmPermission(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.exactAlarmSettings(it)) },
                ),
            )
            add(
                ChecklistItem(
                    "Mostrar sobre otras apps",
                    "Necesario para el aviso de bloqueo de Reels/TikTok.",
                    isChecked = { PermissionStatus.hasOverlayPermission(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.overlaySettings(it)) },
                ),
            )
            add(
                ChecklistItem(
                    "Acceso a datos de uso",
                    "Para medir tu tiempo de pantalla por app.",
                    isChecked = { PermissionStatus.hasUsageAccess(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.usageAccessSettings()) },
                ),
            )
            add(
                ChecklistItem(
                    "Servicio de accesibilidad",
                    "Para poder detectar y limitar Reels/TikTok (nunca lee tus mensajes).",
                    isChecked = { PermissionStatus.hasAccessibilityServiceEnabled(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.accessibilitySettings()) },
                ),
            )
            add(
                ChecklistItem(
                    "Ignorar optimización de batería",
                    "Evita que el sistema mate la app y no suenen las alarmas.",
                    isChecked = { PermissionStatus.hasIgnoreBatteryOptimizations(context) },
                    launch = { PermissionIntents.safeStart(it, PermissionIntents.ignoreBatteryOptimizations(it)) },
                ),
            )
            if (isXiaomi) {
                add(
                    ChecklistItem(
                        "Autoinicio (MIUI)",
                        "Sin esto, MIUI puede impedir que las alarmas suenen tras reiniciar el teléfono. Actívalo manualmente en la lista.",
                        isXiaomiOnly = true,
                        isChecked = { false }, // MIUI no expone una API para verificar esto
                        launch = { PermissionIntents.safeStart(it, PermissionIntents.xiaomiAutoStart(it)) },
                    ),
                )
                add(
                    ChecklistItem(
                        "Mostrar ventanas emergentes en segundo plano (MIUI)",
                        "Sin esto, la alarma puede no reabrirse sola si la mandas a segundo plano sin apagarla.",
                        isXiaomiOnly = true,
                        isChecked = { false },
                        launch = { PermissionIntents.safeStart(it, PermissionIntents.xiaomiBackgroundPopup(it)) },
                    ),
                )
            }
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Permisos") }) }) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            Text(
                if (isXiaomi) {
                    "Tu teléfono es Xiaomi/MIUI: estos ajustes son más estrictos de lo normal en Android. Actívalos todos para que Wake up funcione sin sorpresas."
                } else {
                    "Activa estos permisos para que Wake up funcione de forma confiable."
                },
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(16.dp),
            )
            LazyColumn(modifier = Modifier.weight(1f, fill = true)) {
                items(items) { item -> ChecklistRow(item, context) }
            }
            TextButton(onClick = onDone, modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Listo por ahora")
            }
        }
    }
}

@Composable
private fun ChecklistRow(item: ChecklistItem, context: android.content.Context) {
    val checked = remember(item) { runCatching { item.isChecked() }.getOrDefault(false) }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                if (checked) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (checked) Color(0xFF00BFA6) else MaterialTheme.colorScheme.outline,
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(item.title, style = MaterialTheme.typography.titleMedium)
                Text(item.description, style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = { item.launch(context) }) { Text(if (checked) "Ver" else "Activar") }
        }
    }
}
