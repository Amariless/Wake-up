package com.fritangui.wakeup.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.fritangui.wakeup.alarm.AlarmConstants
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.domain.nextWidgetRefreshBoundary
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arma una alarma exacta para el próximo instante en que empieza o termina una clase (#154), así
 * los widgets de home screen se refrescan justo en ese momento en vez de solo depender del
 * refresco periódico de ~30 min que impone Android en los widgets, o de que el usuario haya hecho
 * algún cambio de datos mientras tanto. Se auto-reprograma: cada refresco (venga de este boundary
 * o de cualquier otro disparador — ver [WidgetRefresher.refreshAll]) vuelve a llamar acá para
 * armar el siguiente cruce, así siempre queda al día con el horario más reciente.
 *
 * No usa `setAlarmClock` (eso mostraría el ícono de alarma en la barra de estado por algo que no
 * es una alarma real para el usuario) — mismo criterio que
 * [com.fritangui.wakeup.alarm.AlarmScheduler.scheduleClassReminder].
 */
@Singleton
class WidgetRefreshScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val subjectRepository: SubjectRepository,
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    suspend fun scheduleNextBoundary() {
        val manager = alarmManager ?: return
        val subjects = subjectRepository.observeWithSessionsForActiveFolders().first()
        val boundary = nextWidgetRefreshBoundary(subjects)
        if (boundary == null) {
            cancel()
            return
        }
        val triggerMillis = boundary.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val pendingIntent = pendingIntent()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.set(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancel() {
        val manager = alarmManager ?: return
        manager.cancel(pendingIntent())
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        context,
        AlarmConstants.REQUEST_CODE_WIDGET_REFRESH_BOUNDARY,
        Intent(context, WidgetRefreshReceiver::class.java).apply {
            action = AlarmConstants.ACTION_WIDGET_REFRESH_BOUNDARY
        },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )
}
