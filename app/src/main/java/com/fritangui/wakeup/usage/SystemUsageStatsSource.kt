package com.fritangui.wakeup.usage

import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import javax.inject.Inject

/** Envoltorio delgado sobre [UsageStatsManager] (requiere el permiso especial "Acceso a datos de uso"). */
class SystemUsageStatsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /** @return minutos de uso en primer plano hoy, por paquete. */
    fun queryTodayUsageMinutesByPackage(): Map<String, Long> {
        val manager = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val zone = TimeZone.currentSystemDefault()
        val startOfDayMillis = Clock.System.todayIn(zone).atStartOfDayIn(zone).toEpochMilliseconds()
        val nowMillis = System.currentTimeMillis()

        val stats = runCatching {
            manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, startOfDayMillis, nowMillis)
        }.getOrNull() ?: return emptyMap()

        val totalsMillis = mutableMapOf<String, Long>()
        for (stat in stats) {
            if (stat.totalTimeInForeground <= 0) continue
            totalsMillis[stat.packageName] = (totalsMillis[stat.packageName] ?: 0L) + stat.totalTimeInForeground
        }
        return totalsMillis.mapValues { it.value / 60_000 }
    }
}
