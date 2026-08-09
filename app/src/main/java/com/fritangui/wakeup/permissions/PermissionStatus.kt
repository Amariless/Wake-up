package com.fritangui.wakeup.permissions

import android.app.AlarmManager
import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import com.fritangui.wakeup.blocking.ReelsBlockAccessibilityService

/**
 * Comprobaciones de permisos "especiales" que Android (y sobre todo MIUI) no
 * conceden con el diálogo estándar de permisos, sino que requieren mandar al
 * usuario a una pantalla concreta de Ajustes. Todo centralizado aquí para que
 * tanto el wizard de onboarding como los banners sueltos en Screen Time/
 * Bloqueo puedan reusar la misma lógica.
 */
object PermissionStatus {

    fun hasNotificationPermission(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun hasExactAlarmPermission(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService<AlarmManager>() ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    fun hasOverlayPermission(context: Context): Boolean = Settings.canDrawOverlays(context)

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService<AppOpsManager>() ?: return false
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName)
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun hasIgnoreBatteryOptimizations(context: Context): Boolean {
        val pm = context.getSystemService<PowerManager>() ?: return false
        return pm.isIgnoringBatteryOptimizations(context.packageName)
    }

    fun hasAccessibilityServiceEnabled(context: Context): Boolean {
        val enabled = Settings.Secure.getInt(context.contentResolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) == 1
        if (!enabled) return false
        val servicesString = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES) ?: return false
        val expectedComponent = "${context.packageName}/${ReelsBlockAccessibilityService::class.java.name}"
        return servicesString.split(':').any { it.equals(expectedComponent, ignoreCase = true) }
    }

    /** Necesario para poder instalar el APK descargado desde el buscador de actualizaciones. */
    fun canInstallPackages(context: Context): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) context.packageManager.canRequestPackageInstalls() else true

    /** No hay API pública para saber si el autostart de MIUI está activo: se le pide al usuario que lo confirme visualmente. */
    fun isXiaomiDevice(): Boolean = Build.MANUFACTURER.equals("Xiaomi", ignoreCase = true) ||
        Build.MANUFACTURER.equals("Redmi", ignoreCase = true) ||
        Build.MANUFACTURER.equals("POCO", ignoreCase = true)
}
