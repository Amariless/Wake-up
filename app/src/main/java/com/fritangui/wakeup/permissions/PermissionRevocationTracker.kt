package com.fritangui.wakeup.permissions

import android.content.Context
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import kotlinx.coroutines.flow.first

/**
 * Detecta cuándo un permiso que SÍ estaba concedido deja de estarlo (p.ej. el usuario apaga el
 * servicio de accesibilidad a mano, o MIUI se lo quita solo tras una limpieza de batería) — a
 * diferencia de simplemente comprobar "¿está concedido ahora?" (que también da falso/negativo la
 * primerísima vez, antes de que el usuario configure nada, y no es lo que se quiere avisar acá).
 * Ver #8.
 */
object PermissionRevocationTracker {

    data class Tracked(val key: String, val label: String, val isGranted: (Context) -> Boolean)

    val tracked: List<Tracked> = listOf(
        Tracked("notifications", "Notificaciones") { PermissionStatus.hasNotificationPermission(it) },
        Tracked("exact_alarms", "Alarmas y recordatorios exactos") { PermissionStatus.hasExactAlarmPermission(it) },
        Tracked("accessibility", "Servicio de accesibilidad (bloqueo de Reels/TikTok)") { PermissionStatus.hasAccessibilityServiceEnabled(it) },
        Tracked("battery", "Ignorar optimización de batería") { PermissionStatus.hasIgnoreBatteryOptimizations(it) },
        Tracked("usage_access", "Acceso a datos de uso") { PermissionStatus.hasUsageAccess(it) },
    )

    /**
     * Compara el estado actual contra el último conocido (guardado en [SettingsDataStore]) y
     * devuelve las etiquetas de los que pasaron de concedido a no-concedido desde la última vez que
     * se llamó esto. Siempre actualiza el estado guardado al actual antes de devolver, así la
     * próxima llamada compara contra ESTE chequeo (se avisa una vez por pérdida, no en cada chequeo
     * mientras siga apagado).
     */
    suspend fun checkForNewlyRevoked(context: Context, settingsDataStore: SettingsDataStore): List<String> {
        val lastGranted = settingsDataStore.lastKnownGrantedPermissionKeys.first()
        val currentGrantedKeys = tracked.filter { runCatching { it.isGranted(context) }.getOrDefault(false) }.map { it.key }.toSet()
        val newlyRevoked = tracked.filter { it.key in lastGranted && it.key !in currentGrantedKeys }
        settingsDataStore.setLastKnownGrantedPermissionKeys(currentGrantedKeys)
        return newlyRevoked.map { it.label }
    }
}
