package com.fritangui.wakeup.usage

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.notifications.NotificationHelper
import com.fritangui.wakeup.permissions.PermissionStatus
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Cada ~15 min: vuelca el uso de pantalla de hoy (vía [SystemUsageStatsSource])
 * a Room, y revisa si alguna [com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity]
 * (p.ej. "redes sociales: 90 min/día") ya se superó para avisar con una notificación.
 */
@HiltWorker
class ScreenTimeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val usageStatsSource: SystemUsageStatsSource,
    private val usageRepository: UsageRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        if (!PermissionStatus.hasUsageAccess(applicationContext)) return Result.success()

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
        return Result.success()
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "screen_time_worker"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<ScreenTimeWorker>(15, TimeUnit.MINUTES).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
