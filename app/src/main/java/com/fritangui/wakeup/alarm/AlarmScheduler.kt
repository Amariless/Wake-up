package com.fritangui.wakeup.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import com.fritangui.wakeup.MainActivity
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_ALARM_FIRE
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_PRE_ALARM_FIRE
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_TASK_REMINDER_FIRE
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_REMINDER_INDEX
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_TASK_ID
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.domain.AlarmTiming
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Instant
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes

/**
 * Envoltorio delgado sobre [AlarmManager]. No conoce Room: recibe entidades ya
 * cargadas y solo se encarga de traducir "cuándo debe sonar" (calculado por
 * [AlarmTiming], que es puro y testeable) en llamadas reales al sistema.
 *
 * Usa `setAlarmClock`, la API pensada específicamente para apps de despertador:
 * está exenta de Doze/App Standby y hace que el sistema muestre el ícono de
 * alarma en la barra de estado (así el usuario confía en que sí va a sonar).
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val alarmManager: AlarmManager? = context.getSystemService()

    fun scheduleAlarm(alarm: AlarmEntity) {
        val manager = alarmManager ?: return
        val trigger = AlarmTiming.nextTrigger(alarm)
        if (trigger == null) {
            cancelAlarm(alarm.id)
            return
        }

        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.mainRequestCode(alarm.id),
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_FIRE
                putExtra(EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val showIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.showIntentRequestCode(alarm.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        manager.setAlarmClock(
            AlarmManager.AlarmClockInfo(trigger.toEpochMilliseconds(), showIntent),
            firePendingIntent,
        )

        schedulePreAlarmNotification(alarm, trigger)
    }

    fun cancelAlarm(alarmId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(
            PendingIntent.getBroadcast(
                context,
                AlarmConstants.mainRequestCode(alarmId),
                Intent(context, AlarmReceiver::class.java).apply { action = ACTION_ALARM_FIRE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
        cancelPreAlarmNotification(alarmId)
    }

    private fun schedulePreAlarmNotification(alarm: AlarmEntity, mainTrigger: Instant) {
        val manager = alarmManager ?: return
        if (alarm.preAlarmNotificationMinutesBefore <= 0) {
            cancelPreAlarmNotification(alarm.id)
            return
        }
        val preTrigger = mainTrigger.minus(alarm.preAlarmNotificationMinutesBefore.minutes)
        if (preTrigger <= kotlinx.datetime.Clock.System.now()) {
            // Falta menos de preAlarmNotificationMinutesBefore para que suene: no tiene sentido avisar antes.
            cancelPreAlarmNotification(alarm.id)
            return
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.preAlarmRequestCode(alarm.id),
            Intent(context, PreAlarmReceiver::class.java).apply {
                action = ACTION_PRE_ALARM_FIRE
                putExtra(EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            // Sin permiso de alarmas exactas (raro: ver XiaomiPermissionHelper), degradamos a inexacto.
            manager.set(AlarmManager.RTC_WAKEUP, preTrigger.toEpochMilliseconds(), pendingIntent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, preTrigger.toEpochMilliseconds(), pendingIntent)
        }
    }

    private fun cancelPreAlarmNotification(alarmId: Long) {
        val manager = alarmManager ?: return
        manager.cancel(
            PendingIntent.getBroadcast(
                context,
                AlarmConstants.preAlarmRequestCode(alarmId),
                Intent(context, PreAlarmReceiver::class.java).apply { action = ACTION_PRE_ALARM_FIRE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    fun scheduleTaskReminder(taskId: Long, reminderIndex: Int, triggerAt: Instant) {
        val manager = alarmManager ?: return
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.taskReminderRequestCode(taskId, reminderIndex),
            Intent(context, TaskReminderReceiver::class.java).apply {
                action = ACTION_TASK_REMINDER_FIRE
                putExtra(EXTRA_TASK_ID, taskId)
                putExtra(EXTRA_REMINDER_INDEX, reminderIndex)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
            manager.set(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilliseconds(), pendingIntent)
        } else {
            manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt.toEpochMilliseconds(), pendingIntent)
        }
    }

    fun cancelTaskReminder(taskId: Long, reminderIndex: Int) {
        val manager = alarmManager ?: return
        manager.cancel(
            PendingIntent.getBroadcast(
                context,
                AlarmConstants.taskReminderRequestCode(taskId, reminderIndex),
                Intent(context, TaskReminderReceiver::class.java).apply { action = ACTION_TASK_REMINDER_FIRE },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            ),
        )
    }

    /** Cancela hasta [maxReminders] índices de recordatorio de una tarea (se usa al borrar/editar). */
    fun cancelAllTaskReminders(taskId: Long, maxReminders: Int = 8) {
        for (i in 0 until maxReminders) cancelTaskReminder(taskId, i)
    }

    /**
     * Reprograma la MISMA alarma para dentro de [minutesFromNow] minutos (botón
     * "posponer"). Reutiliza a propósito el mismo PendingIntent que la
     * ocurrencia regular: al sonar el snooze, [RingingForegroundService] vuelve
     * a llamar a [scheduleAlarm], que re-arma la siguiente ocurrencia real, así
     * que el estado se autocorrige en cuanto la alarma pospuesta suena.
     */
    fun scheduleSnooze(alarmId: Long, minutesFromNow: Int = 5) {
        val manager = alarmManager ?: return
        val triggerAtMillis = System.currentTimeMillis() + minutesFromNow * 60_000L
        val firePendingIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.mainRequestCode(alarmId),
            Intent(context, AlarmReceiver::class.java).apply {
                action = ACTION_ALARM_FIRE
                putExtra(EXTRA_ALARM_ID, alarmId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val showIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.showIntentRequestCode(alarmId),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), firePendingIntent)
    }

    fun canScheduleExactAlarms(): Boolean {
        val manager = alarmManager ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) manager.canScheduleExactAlarms() else true
    }
}
