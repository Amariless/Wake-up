package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_PRE_ALARM_FIRE
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_SKIP_NEXT_OCCURRENCE
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Dos responsabilidades relacionadas con el aviso previo (T-60min por
 * defecto): mostrar la notificación cuando corresponde, y manejar el botón
 * "apagar solo esta vez" de esa notificación (marca [AlarmEntity.skipNextOccurrence]
 * sin desactivar la alarma; la propia [RingingForegroundService] lo consume la
 * próxima vez que suene).
 */
@AndroidEntryPoint
class PreAlarmReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                when (intent.action) {
                    ACTION_PRE_ALARM_FIRE -> {
                        val alarm = alarmRepository.getById(alarmId) ?: return@launch
                        if (alarm.isEnabled && !alarm.skipNextOccurrence) {
                            notificationHelper.notifyPreAlarm(alarm)
                        }
                    }
                    ACTION_SKIP_NEXT_OCCURRENCE -> {
                        alarmRepository.setSkipNext(alarmId, true)
                        notificationHelper.cancelPreAlarmNotification(alarmId)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
