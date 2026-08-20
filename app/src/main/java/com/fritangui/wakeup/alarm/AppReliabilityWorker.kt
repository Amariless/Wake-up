package com.fritangui.wakeup.alarm

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.domain.AlarmTiming
import com.fritangui.wakeup.notifications.NotificationHelper
import com.fritangui.wakeup.permissions.AlarmVolumeStatus
import com.fritangui.wakeup.permissions.PermissionRevocationTracker
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.hours

/**
 * Cada hora, dos chequeos de "algo puede impedir que tu alarma suene" (#8, #9):
 * 1. ¿Algún permiso que estaba concedido se apagó solo? (accesibilidad, alarmas exactas, etc.)
 * 2. ¿El volumen de alarma está por debajo de la mitad Y hay una alarma real (no un simple
 *    recordatorio) por sonar dentro de las próximas [SettingsDataStore.lowAlarmVolumeWarningHoursAhead] horas?
 *
 * Corre en segundo plano (a diferencia del banner de Inicio, que solo se ve si se abre la app) para
 * que el aviso llegue aunque el usuario no haya abierto Wake up justo antes de que suene la alarma.
 */
@HiltWorker
class AppReliabilityWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val settingsDataStore: SettingsDataStore,
    private val alarmRepository: AlarmRepository,
    private val notificationHelper: NotificationHelper,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        checkRevokedPermissions()
        checkLowVolumeUpcomingAlarm()
        return Result.success()
    }

    private suspend fun checkRevokedPermissions() {
        val revoked = PermissionRevocationTracker.checkForNewlyRevoked(applicationContext, settingsDataStore)
        if (revoked.isNotEmpty()) notificationHelper.notifyPermissionsRevoked(revoked)
    }

    private suspend fun checkLowVolumeUpcomingAlarm() {
        if (!settingsDataStore.lowAlarmVolumeWarningEnabled.first()) return
        if (!AlarmVolumeStatus.isLow(applicationContext)) return

        val hoursAhead = settingsDataStore.lowAlarmVolumeWarningHoursAhead.first()
        val now = Clock.System.now()
        val windowEnd = now + hoursAhead.hours
        val nearestTrigger = alarmRepository.getAllActiveEnabled()
            .filter { it.kind == AlarmKind.ALARM }
            .mapNotNull { AlarmTiming.nextTrigger(it, now = now) }
            .filter { it in now..windowEnd }
            .minOrNull() ?: return

        // No repetir el mismo aviso en cada corrida de la hora si nada cambió (ni la alarma más
        // próxima ni el volumen) — solo se re-notifica si cambia CUÁL es la próxima alarma en juego.
        val lastNotified = settingsDataStore.lastNotifiedLowVolumeTriggerMillis.first()
        if (lastNotified == nearestTrigger.toEpochMilliseconds()) return

        notificationHelper.notifyLowAlarmVolume(hoursAhead)
        settingsDataStore.setLastNotifiedLowVolumeTriggerMillis(nearestTrigger.toEpochMilliseconds())
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "app_reliability_worker"

        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<AppReliabilityWorker>(1, TimeUnit.HOURS).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
