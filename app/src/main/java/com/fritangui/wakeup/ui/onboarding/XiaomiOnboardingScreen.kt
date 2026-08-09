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
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus

private data class ChecklistItem(
    val title: String,
    val description: String,
    /** null = Android no expone forma de saberlo (p.ej. permisos propios de MIUI); no es lo mismo que "no concedido". */
    val isChecked: (() -> Boolean)?,
    val launch: (android.content.Context) -> Unit,
    /** Solo para items con [isChecked] null: le permite al usuario confirmar a mano que ya lo activó. */
    val manualConfirmed: Boolean? = null,
    val onManualConfirmChange: ((Boolean) -> Unit)? = null,
)

private const val RESTRICTED_SETTINGS_HINT = "Si Android dice \"por seguridad esta opción no está disponible\": " +
    "ve a Ajustes del sistema → Apps → Wake up → toca el menú ⋮ (arriba a la derecha) → " +
    "\"Permitir configuración restringida\", y vuelve a intentar. Es una protección de Android para apps instaladas fuera de Play Store, no un problema de la app."

/**
 * Wizard de permisos: no hay API pública para forzar ninguno de estos ajustes,
 * así que se guía al usuario paso a paso y se le deja auto-verificar (los que
 * sí se pueden comprobar por código, como notificaciones o accesibilidad, se
 * marcan solos al volver a la pantalla).
 */
@Composable
fun XiaomiOnboardingScreen(onDone: () -> Unit, viewModel: XiaomiOnboardingViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val isXiaomi = remember { PermissionStatus.isXiaomiDevice() }
    var refreshTick by remember { mutableStateOf(0) }
    val autoStartConfirmed by viewModel.autoStartConfirmed.collectAsState()
    val backgroundPopupConfirmed by viewModel.backgroundPopupConfirmed.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { refreshTick++ }

    val items = remember(refreshTick, autoStartConfirmed, backgroundPopupConfirmed) {
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
                    "Para detectar y limitar Reels/TikTok (nunca lee tus mensajes). $RESTRICTED_SETTINGS_HINT",
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
                        "Sin esto, MIUI puede impedir que las alarmas suenen tras reiniciar el teléfono. MIUI no deja que ninguna " +
                            "app (ni esta) revise este ajuste por código: ábrelo, confirma que el interruptor esté activado y " +
                            "marca la casilla de abajo para no verlo en amarillo cada vez.",
                        isChecked = null,
                        launch = { PermissionIntents.safeStart(it, PermissionIntents.xiaomiAutoStart(it)) },
                        manualConfirmed = autoStartConfirmed,
                        onManualConfirmChange = viewModel::setAutoStartConfirmed,
                    ),
                )
                add(
                    ChecklistItem(
                        "Mostrar ventanas emergentes en segundo plano (MIUI)",
                        "Sin esto, la alarma puede no reabrirse sola si la mandas a segundo plano sin apagarla. Tampoco se puede " +
                            "verificar por código: confirma que esté activado y marca la casilla de abajo.",
                        isChecked = null,
                        launch = { PermissionIntents.safeStart(it, PermissionIntents.xiaomiBackgroundPopup(it)) },
                        manualConfirmed = backgroundPopupConfirmed,
                        onManualConfirmChange = viewModel::setBackgroundPopupConfirmed,
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
    // Tres estados: concedido (✓ verde), no concedido (○ gris), y "no se puede saber" (? ámbar
    // neutro). Una confirmación manual solo puede empujar a "concedido" — nunca a "no concedido",
    // porque seguimos sin poder comprobarlo de verdad; si el usuario no ha marcado la casilla,
    // se queda en ámbar en vez de caer a gris (que antes se leía como "seguro que está apagado").
    val checked = when {
        item.manualConfirmed == true -> true
        item.isChecked != null -> remember(item) { runCatching { item.isChecked.invoke() }.getOrDefault(false) }
        else -> null
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    when (checked) {
                        true -> Icons.Default.CheckCircle
                        false -> Icons.Default.RadioButtonUnchecked
                        null -> Icons.Default.HelpOutline
                    },
                    contentDescription = null,
                    tint = when (checked) {
                        true -> Color(0xFF00BFA6)
                        false -> MaterialTheme.colorScheme.outline
                        null -> Color(0xFFFFC857)
                    },
                )
                Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                    Text(item.title, style = MaterialTheme.typography.titleMedium)
                    Text(item.description, style = MaterialTheme.typography.bodyMedium)
                }
                TextButton(onClick = { item.launch(context) }) { Text(if (checked == true) "Ver" else "Activar") }
            }
            if (item.onManualConfirmChange != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 36.dp)) {
                    Checkbox(checked = item.manualConfirmed == true, onCheckedChange = item.onManualConfirmChange)
                    Text("Ya lo revisé y está activado", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
