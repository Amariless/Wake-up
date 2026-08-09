package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Disparado por AlarmManager exactamente a la hora de la alarma/recordatorio. Debe ser lo más
 * ligero posible (los BroadcastReceiver solo tienen unos segundos antes de que el sistema los
 * considere "ANR"), así que usa `goAsync()` para poder consultar Room sin bloquear el hilo
 * principal más de la cuenta.
 *
 * - [AlarmKind.ALARM]: arranca el foreground service que de verdad hace sonar la alarma a pantalla
 *   completa (todo el trabajo pesado vive ahí, ver [RingingForegroundService]).
 * - [AlarmKind.REMINDER]: no necesita ni pantalla completa ni foreground service — solo postea una
 *   notificación normal con su propio sonido y reprograma/borra según corresponda, aquí mismo.
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return

        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val alarm = alarmRepository.getById(alarmId) ?: return@launch
                when (alarm.kind) {
                    AlarmKind.ALARM -> {
                        val serviceIntent = Intent(context, RingingForegroundService::class.java).apply {
                            action = RingingForegroundService.ACTION_START_RINGING
                            putExtra(EXTRA_ALARM_ID, alarmId)
                        }
                        ContextCompat.startForegroundService(context, serviceIntent)
                    }
                    AlarmKind.REMINDER -> {
                        notificationHelper.notifyReminder(alarm)
                        if (alarm.deleteAfterRing) {
                            alarmScheduler.cancelAlarm(alarm.id)
                            alarmRepository.delete(alarm)
                        } else {
                            alarmScheduler.scheduleAlarm(alarm)
                            if (alarm.repeatDaysBitmask == 0) {
                                alarmRepository.setEnabled(alarm.id, false)
                            }
                            alarmRepository.setLastTriggered(alarm.id)
                        }
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
