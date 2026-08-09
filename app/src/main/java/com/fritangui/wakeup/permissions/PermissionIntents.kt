package com.fritangui.wakeup.permissions

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings

/**
 * Construye los intents para llevar al usuario a cada pantalla de permisos.
 * Los específicos de MIUI (autostart, ventanas emergentes en segundo plano)
 * no son API pública de Android: cambian entre versiones de MIUI y a veces
 * ni siquiera existen en un teléfono dado, así que cada uno tiene un
 * fallback seguro a los Ajustes generales de la app.
 */
object PermissionIntents {

    fun openAppSettings(context: Context): Intent =
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))

    fun notificationSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        } else {
            openAppSettings(context)
        }

    fun exactAlarmSettings(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.fromParts("package", context.packageName, null))
        } else {
            openAppSettings(context)
        }

    fun overlaySettings(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.fromParts("package", context.packageName, null))

    fun usageAccessSettings(): Intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun accessibilitySettings(): Intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)

    fun ignoreBatteryOptimizations(context: Context): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.fromParts("package", context.packageName, null))

    /** Pantalla de "instalar apps desconocidas" para esta app, necesaria para instalar el APK actualizado. */
    fun manageUnknownAppSources(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.fromParts("package", context.packageName, null))
        } else {
            openAppSettings(context)
        }

    /** Panel de autostart de MIUI. Varía entre versiones de MIUI/HyperOS; cae al listado general si no existe. */
    fun xiaomiAutoStart(context: Context): Intent {
        val candidates = listOf(
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity2"),
        )
        return firstResolvable(context, candidates) ?: openAppSettings(context)
    }

    /** Permiso MIUI "mostrar ventanas emergentes en segundo plano", necesario para reabrir la alarma sin desbloquear. */
    fun xiaomiBackgroundPopup(context: Context): Intent {
        val candidates = listOf(
            Intent("miui.intent.action.APP_PERM_EDITOR")
                .setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.PermissionsEditorActivity")
                .putExtra("extra_pkgname", context.packageName),
            Intent().setClassName("com.miui.securitycenter", "com.miui.permcenter.permissions.AppPermissionsEditorActivity")
                .putExtra("extra_pkgname", context.packageName),
        )
        return firstResolvable(context, candidates) ?: openAppSettings(context)
    }

    private fun firstResolvable(context: Context, candidates: List<Intent>): Intent? =
        candidates.firstOrNull { intent ->
            runCatching { intent.resolveActivity(context.packageManager) != null }.getOrDefault(false)
        }

    /** Envoltorio seguro: si el intent no se puede lanzar en este dispositivo, cae a Ajustes de la app. */
    fun safeStart(context: Context, intent: Intent) {
        try {
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (_: ActivityNotFoundException) {
            context.startActivity(openAppSettings(context).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
