package com.fritangui.wakeup.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
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
import com.fritangui.wakeup.alarm.sound.NotificationSounds
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import com.fritangui.wakeup.widget.WidgetDeepLink
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/** Centraliza la construcción de todas las notificaciones de la app. */
@Singleton
class NotificationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) {
    private val manager = NotificationManagerCompat.from(context)

    companion object {
        const val TIMER_RUNNING_NOTIF_ID = 60_000
        const val TIMER_RINGING_NOTIF_ID = 60_001
    }

    /**
     * A propósito NO tiene una acción de "Apagar": si el usuario configuró un
     * reto, apagar la alarma con un solo toque desde la notificación sería
     * hacer trampa (justo lo que no queremos si la idea es asegurarnos de que
     * de verdad se despertó). Solo se puede apagar completando el reto dentro
     * de [com.fritangui.wakeup.alarm.AlarmRingingActivity].
     */
    fun notifyRinging(alarm: AlarmEntity, fullScreenPendingIntent: PendingIntent) =
        NotificationCompat.Builder(context, WakeUpApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(alarm.label.ifBlank { "Alarma" })
            .setContentText("Toca para abrir")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()
            .also { manager.notify(AlarmConstants.NOTIF_ID_RINGING_BASE + alarm.id.toInt(), it) }

    fun cancelRinging(alarmId: Long) {
        manager.cancel(AlarmConstants.NOTIF_ID_RINGING_BASE + alarmId.toInt())
    }

    /**
     * `suspend` porque necesita leer la preferencia de formato de hora (12h/24h) antes de armar el
     * texto — ambos llamadores (PreAlarmReceiver, DevToolsViewModel) ya corren en una corrutina.
     */
    suspend fun notifyPreAlarm(alarm: AlarmEntity) {
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
        // La hora exacta va en el TÍTULO (siempre visible aunque la notificación esté colapsada, a
        // diferencia del contentText, que algunos launchers/OEMs ocultan hasta expandir) — antes el
        // título solo decía "suena en 60 min" (el valor fijo configurado, no algo que dijera A QUÉ
        // HORA sonará de verdad) y la hora quedaba solo en la segunda línea.
        val use24Hour = settingsDataStore.use24HourFormat.first()
        val timeText = formatClockTime(alarm.hour, alarm.minute, use24Hour) +
            (amPmSuffix(alarm.hour, use24Hour)?.let { " $it" } ?: "")
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_PRE_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("${alarm.label.ifBlank { "Alarma" }} suena a las $timeText")
            .setContentText("En ${alarm.preAlarmNotificationMinutesBefore} min")
            .setColor(0xFF7C9CFF.toInt())
            .setCategory(NotificationCompat.CATEGORY_ALARM)
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

    /**
     * "Recordatorio" ([AlarmEntity.kind] == REMINDER): a diferencia de [notifyRinging], es una
     * notificación normal y corriente — se puede descartar con un swipe, no bloquea la pantalla ni
     * exige ningún reto. Cada sonido del banco de notificaciones vive en su propio canal: en
     * Android 8+ el sonido de un canal ya creado no se puede cambiar después, así que en vez de un
     * único canal "recordatorios" se crea uno por sonido (se identifican por su propio nombre de
     * archivo, no cambian entre builds) y se reutiliza.
     */
    fun notifyReminder(alarm: AlarmEntity) {
        val openIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.showIntentRequestCode(alarm.id),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val soundUri = alarm.soundUri ?: NotificationSounds.defaultSoundUriFor(context)
        val channelId = ensureReminderChannel(soundUri)
        val hh = alarm.hour.toString().padStart(2, '0')
        val mm = alarm.minute.toString().padStart(2, '0')
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle(alarm.label.ifBlank { "Recordatorio" })
            .setContentText("$hh:$mm")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(Uri.parse(soundUri))
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(AlarmConstants.NOTIF_ID_REMINDER_BASE + alarm.id.toInt(), notification)
    }

    private fun ensureReminderChannel(soundUri: String): String {
        val channelId = "channel_reminder_${soundUri.hashCode()}"
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return channelId
        val platformManager = context.getSystemService(NotificationManager::class.java) ?: return channelId
        if (platformManager.getNotificationChannel(channelId) != null) return channelId
        val channel = NotificationChannel(channelId, "Recordatorios", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Recordatorios con sonido de notificación personalizado"
            setSound(
                Uri.parse(soundUri),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        platformManager.createNotificationChannel(channel)
        return channelId
    }

    fun cancelReminder(alarmId: Long) {
        manager.cancel(AlarmConstants.NOTIF_ID_REMINDER_BASE + alarmId.toInt())
    }

    /**
     * Antes solo decía "Tarea próxima a vencer" (sin decir cuánto faltaba) y abría la app en
     * Inicio en vez de la tarea concreta (#136). Ahora reutiliza [WidgetDeepLink] — el mismo
     * mecanismo que ya usan los widgets — para llevar directo a la tarea, y calcula cuánto falta
     * al momento de sonar (no hace falta guardar qué offset la disparó: a esa hora exacta, lo que
     * quede hasta el vencimiento YA es la respuesta correcta).
     */
    fun notifyTaskReminder(task: TaskEntity, subjectName: String?) {
        val openIntent = PendingIntent.getActivity(
            context,
            task.id.toInt(),
            WidgetDeepLink.taskIntent(context, task.folderId, task.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val title = if (subjectName != null) "$subjectName: ${task.title}" else task.title
        val timeUntilText = task.dueAtEpochMillis?.let { formatTimeUntilDue(System.currentTimeMillis(), it) }
            ?: "Tarea próxima a vencer"
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_TASK_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification_task)
            .setContentTitle(title)
            .setContentText(timeUntilText)
            // Resalta más que la notificación "plana" anterior (#137): color propio de la app,
            // categoría de recordatorio (algunos launchers la agrupan/priorizan distinto) y prioridad
            // alta en vez de la default.
            .setColor(0xFF7C9CFF.toInt())
            .setColorized(false)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(AlarmConstants.NOTIF_ID_TASK_REMINDER_BASE + task.id.toInt(), notification)
    }

    /** "Vence en 1 semana" / "Vence mañana" / "Vence en 3 h" — ver [notifyTaskReminder]. */
    private fun formatTimeUntilDue(nowMillis: Long, dueMillis: Long): String {
        val diffMinutes = ((dueMillis - nowMillis) / 60_000L).coerceAtLeast(0L)
        val diffHours = diffMinutes / 60
        val diffDays = diffMinutes / (24 * 60)
        return when {
            diffMinutes < 60 -> "Vence en menos de una hora"
            diffDays == 0L -> "Vence en $diffHours h"
            diffDays == 1L -> "Vence mañana"
            diffDays < 7 -> "Vence en $diffDays días"
            diffDays % 7 == 0L -> {
                val weeks = diffDays / 7
                "Vence en $weeks semana${if (weeks > 1) "s" else ""}"
            }
            else -> "Vence en $diffDays días"
        }
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

    /** Sin acción de "Apagar" por la misma razón que [notifyRinging]: no debe poder saltarse el reto. */
    fun buildTimerRingingNotification(fullScreenPendingIntent: PendingIntent) =
        NotificationCompat.Builder(context, WakeUpApp.CHANNEL_ALARM)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("¡Temporizador terminado!")
            .setContentText("Toca para abrir")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .setContentIntent(fullScreenPendingIntent)
            .setOngoing(true)
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
            .setColor(0xFF7C9CFF.toInt())
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(9_000_000 + label.hashCode(), notification)
    }

    /** Aviso global de "tu próxima clase empieza en X min" (#144), configurable en Ajustes. */
    fun notifyNextClass(subjectName: String, room: String, minutesBefore: Int, folderId: Long, subjectId: Long) {
        val openIntent = PendingIntent.getActivity(
            context,
            AlarmConstants.NOTIF_ID_CLASS_REMINDER_BASE + subjectId.toInt(),
            com.fritangui.wakeup.widget.WidgetDeepLink.subjectIntent(context, folderId, subjectId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, WakeUpApp.CHANNEL_NEXT_CLASS)
            .setSmallIcon(R.drawable.ic_notification_alarm)
            .setContentTitle("$subjectName empieza en $minutesBefore min")
            .setContentText(if (room.isNotBlank()) "Salón $room" else "Prepárate")
            .setColor(0xFF7C9CFF.toInt())
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(AlarmConstants.NOTIF_ID_CLASS_REMINDER_BASE + subjectId.toInt(), notification)
    }
}
