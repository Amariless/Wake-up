package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_SESSION_ID
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Aviso global de "tu próxima clase empieza en X min" (#144). A diferencia de [TaskReminderReceiver]
 * (un disparo por recordatorio, ya calculados todos de antemano), esta sesión ocurre cada semana:
 * después de mostrar el aviso, [AlarmScheduler.scheduleClassReminder] se vuelve a llamar acá mismo
 * para armar la ocurrencia de la semana siguiente — así se auto-perpetúa sin depender de que la app
 * esté abierta o de un worker periódico.
 */
@AndroidEntryPoint
class ClassReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var subjectRepository: SubjectRepository
    @Inject lateinit var settingsDataStore: SettingsDataStore
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onReceive(context: Context, intent: Intent) {
        val sessionId = intent.getLongExtra(EXTRA_SESSION_ID, -1L)
        if (sessionId < 0) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val minutesBefore = settingsDataStore.nextClassNotificationMinutesBefore.first()
                // El ajuste se pudo haber apagado (o la sesión/materia se pudo haber borrado) desde
                // que se armó esta alarma — en ese caso no se avisa nada y tampoco se reprograma:
                // se autocorrige solo, sin dejar una alarma fantasma dando vueltas.
                if (minutesBefore <= 0) return@launch
                val session = subjectRepository.getSessionById(sessionId) ?: return@launch
                val subject = subjectRepository.getSubjectByIdOnce(session.subjectId) ?: return@launch

                notificationHelper.notifyNextClass(
                    subjectName = subject.name,
                    room = session.room,
                    minutesBefore = minutesBefore,
                    folderId = subject.folderId,
                    subjectId = subject.id,
                )
                // Rearma la misma sesión para su ocurrencia de la semana que viene.
                alarmScheduler.scheduleClassReminder(session, minutesBefore)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
