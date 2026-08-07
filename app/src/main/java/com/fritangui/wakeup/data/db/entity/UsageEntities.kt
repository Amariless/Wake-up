package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/** Snapshot diario de uso de una app, agregado desde UsageStatsManager por [com.fritangui.wakeup.usage.ScreenTimeWorker]. */
@Entity(tableName = "app_usage_daily", primaryKeys = ["dateEpochDay", "packageName"])
data class AppUsageDailyEntity(
    val dateEpochDay: Long,
    val packageName: String,
    val minutesUsed: Long,
    val lastUpdatedEpochMillis: Long,
)

/**
 * Regla de aviso: cuando el uso acumulado hoy de cualquiera de [packageNames]
 * supera [dailyThresholdMinutes], se dispara una notificación (p. ej. "llevas
 * 45 min en redes sociales hoy").
 */
@Entity(tableName = "usage_alert_rules")
data class UsageAlertRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val packageNames: List<String>,
    val dailyThresholdMinutes: Int,
    val isEnabled: Boolean = true,
)

/** Qué "superficie" de una app se bloquea. Hoy solo Reels de Instagram está implementado con detección real. */
enum class BlockSurface {
    INSTAGRAM_REELS,
    TIKTOK_FOR_YOU,
    GENERIC_APP_TIME_LIMIT,
}

/**
 * Regla de bloqueo tipo "Scroll Guard": [dailyLimitMinutes] de tiempo permitido
 * por día en esa superficie antes de mostrar el overlay de bloqueo. No bloquea
 * el resto de la app (p.ej. los DMs de Instagram siguen funcionando).
 */
@Entity(tableName = "block_rules")
data class BlockRuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val packageName: String,
    val surface: BlockSurface,
    val dailyLimitMinutes: Int,
    val isEnabled: Boolean = true,
)

/**
 * Tiempo acumulado hoy dentro de una superficie bloqueable específica (p.ej.
 * "Reels de Instagram"), medido por [com.fritangui.wakeup.blocking.ReelsBlockAccessibilityService].
 * Independiente de [AppUsageDailyEntity], que mide uso general por app vía
 * UsageStatsManager y no puede distinguir "estoy en Reels" de "estoy en el feed normal".
 */
@Entity(tableName = "block_surface_usage", primaryKeys = ["dateEpochDay", "surface"])
data class BlockSurfaceUsageEntity(
    val dateEpochDay: Long,
    val surface: BlockSurface,
    val accumulatedMillis: Long,
)
