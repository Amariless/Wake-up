package com.fritangui.wakeup.usage

import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.notifications.NotificationHelper
import com.fritangui.wakeup.permissions.PermissionStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import android.content.Context
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Vuelca el uso de pantalla de hoy a Room y revisa umbrales. Antes esta lógica vivía solo dentro
 * de [ScreenTimeWorker], que corre cada ~15 min vía WorkManager — en MIUI ese trabajo en segundo
 * plano puede retrasarse mucho o directamente no correr (batería/autoinicio), así que la pantalla
 * de Bienestar se veía desactualizada. Con esto, la propia pantalla puede pedir un refresco
 * inmediato al abrirse, sin depender solo del scheduling en segundo plano.
 */
@Singleton
class ScreenTimeRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageStatsSource: SystemUsageStatsSource,
    private val usageRepository: UsageRepository,
    private val notificationHelper: NotificationHelper,
) {
    suspend fun refreshNow() {
        if (!PermissionStatus.hasUsageAccess(context)) return

        val today = todayEpochDay()
        val minutesByPackage = usageStatsSource.queryTodayUsageMinutesByPackage()
        val now = System.currentTimeMillis()
        usageRepository.saveDailySnapshot(
            minutesByPackage.map { (pkg, minutes) -> AppUsageDailyEntity(today, pkg, minutes, now) },
        )

        usageRepository.getEnabledAlertRules().forEach { rule ->
            val total = rule.packageNames.sumOf { minutesByPackage[it] ?: 0L }
            if (total >= rule.dailyThresholdMinutes) {
                notificationHelper.notifyUsageThreshold(rule.label, total, rule.dailyThresholdMinutes)
            }
        }
    }
}
