package com.fritangui.wakeup.notifications

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.fritangui.wakeup.MainActivity
import com.fritangui.wakeup.R
import com.fritangui.wakeup.WakeUpApp
import com.fritangui.wakeup.alarm.AlarmConstants
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_SKIP_NEXT_OCCURRENCE
import com.fritangui.wakeup.alarm.AlarmConstants.ACTION_STOP_RINGING
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.alarm.PreAlarmReceiver
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Centraliza la construcción de todas las notificaciones de la app. */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val manager = NotificationManagerCompat.from(context)

    companion object {
        const val TIMER_RUNNING_NOTIF_ID = 60_000
        const val TIMER_RINGING_NOTIF_ID = 60_001
    }

    fun notifyRinging(alarm: AlarmEntity, fullScreenPendingIntent: PendingIntent, stopPendingIntent: PendingIntent) =
        NotificationCompat.Builder(context, WakeUpApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(alarm.label.ifBlank { "Alarma" })
            .setContentText("Toca para abrir y apagar la alarma")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_notification_alarm, "Apagar", stopPendingIntent)
            .build()
            .also { manager.notify(AlarmConstants.NOTIF_ID_RINGING_BASE + alarm.id.toInt(), it) }

    fun cancelRinging(alarmId: Long) {
        manager.cancel(AlarmConstants.NOTIF_ID_RINGING_BASE + alarmId.toInt())
    }

    fun notifyPreAlarm(alarm: AlarmEntity) {
        val openIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.showIntentRequestCode(alarm.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val skipIntent = PendingIntent.getBroadcast(
            context,
            AlarmConstants.preAlarmRequestCode(alarm.id) + 1,
            Intent(context, PreAlarmReceiver::class.java).apply {
                action = ACTION_SKIP_NEXT_OCCURRENCE
                putExtra(EXTRA_ALARM_ID, alarm.id)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val hh = alarm.hour.toString().padStart(2, '0')
        val mm = alarm.minute.toString().padStart(2, '0')
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_PRE_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("${alarm.label.ifBlank { "Alarma" }} suena en ${alarm.preAlarmNotificationMinutesBefore} min")
            .setContentText("Programada para las $hh:$mm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_notification_alarm, "Apagar solo esta vez", skipIntent)
            .build()
        manager.notify(AlarmConstants.NOTIF_ID_PRE_ALARM_BASE + alarm.id.toInt(), notification)
    }

    fun cancelPreAlarmNotification(alarmId: Long) {
        manager.cancel(AlarmConstants.NOTIF_ID_PRE_ALARM_BASE + alarmId.toInt())
    }

    fun notifyTaskReminder(task: TaskEntity, subjectName: String?) {
        val openIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (subjectName != null) "$subjectName: ${task.title}" else task.title
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_task)
            .setContentTitle(title)
            .setContentText("Tarea próxima a vencer")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(AlarmConstants.NOTIF_ID_TASK_REMINDER_BASE + task.id.toInt(), notification)
    }

    fun buildTimerRunningNotification(remainingMillis: Long, isPaused: Boolean, pausePendingIntent: PendingIntent, cancelPendingIntent: PendingIntent) =
        NotificationCompat.Builder(context, WakeUpApp.CHANNEL_TIMER_RUNNING)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(if (isPaused) "Temporizador en pausa" else "Temporizador en curso")
            .setContentText(formatRemaining(remainingMillis))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(R.drawable.ic_notification_alarm, if (isPaused) "Reanudar" else "Pausar", pausePendingIntent)
            .addAction(R.drawable.ic_notification_alarm, "Cancelar", cancelPendingIntent)
            .build()

    fun notifyTimerRunning(remainingMillis: Long, isPaused: Boolean, pausePendingIntent: PendingIntent, cancelPendingIntent: PendingIntent) {
        manager.notify(
            TIMER_RUNNING_NOTIF_ID,
            buildTimerRunningNotification(remainingMillis, isPaused, pausePendingIntent, cancelPendingIntent),
        )
    }

    fun cancelTimerRunningNotification() = manager.cancel(TIMER_RUNNING_NOTIF_ID)

    fun buildTimerRingingNotification(fullScreenPendingIntent: PendingIntent, stopPendingIntent: PendingIntent) =
        NotificationCompat.Builder(context, WakeUpApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("¡Temporizador terminado!")
            .setContentText("Toca para abrir y apagarlo")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .addAction(R.drawable.ic_notification_alarm, "Apagar", stopPendingIntent)
            .build()

    fun cancelTimerRinging() = manager.cancel(TIMER_RINGING_NOTIF_ID)

    private fun formatRemaining(millis: Long): String {
        val totalSeconds = millis / 1000
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return "%02d:%02d".format(m, s)
    }

    fun notifyUsageThreshold(label: String, minutesUsed: Long, thresholdMinutes: Int) {
        val openIntent = PendingIntent.getActivity(
            context,
            9_000_000 + label.hashCode(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_USAGE_NAG)
            .setSmallIcon(R.drawable.ic_notification_usage)
            .setContentTitle("Llevas $minutesUsed min hoy en $label")
            .setContentText("Tu límite configurado es de $thresholdMinutes min")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(9_000_000 + label.hashCode(), notification)
    }
}
