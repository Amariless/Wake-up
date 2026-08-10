package com.fritangui.wakeup.usage

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.fritangui.wakeup.widget.WidgetRefresher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Cada ~15 min (el mínimo que permite WorkManager para trabajo periódico): dispara
 * [ScreenTimeRefresher.refreshNow] para volcar el uso de pantalla de hoy a Room, y refresca el
 * widget de tiempo de pantalla con esos datos nuevos. El aviso de límite de redes sociales ya NO
 * se dispara acá (ver ReelsBlockAccessibilityService.checkAlertRules). La pantalla de Bienestar
 * también llama a [ScreenTimeRefresher] directo al abrirse (ver ScreenTimeViewModel), porque en
 * MIUI este trabajo en segundo plano no es confiable.
 */
@HiltWorker
class ScreenTimeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val refresher: ScreenTimeRefresher,
    private val widgetRefresher: WidgetRefresher,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        refresher.refreshNow()
        widgetRefresher.refreshAll()
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
