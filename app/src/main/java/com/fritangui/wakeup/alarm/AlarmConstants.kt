package com.fritangui.wakeup.alarm

/** Extras/constantes compartidas entre receivers, servicio y activity de alarma. */
object AlarmConstants {
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_TASK_ID = "extra_task_id"
    const val EXTRA_REMINDER_INDEX = "extra_reminder_index"
    const val EXTRA_FROM_NOTIFICATION_ACTION = "extra_from_notification_action"
    const val EXTRA_SESSION_ID = "extra_session_id"

    const val ACTION_ALARM_FIRE = "com.fritangui.wakeup.action.ALARM_FIRE"
    const val ACTION_PRE_ALARM_FIRE = "com.fritangui.wakeup.action.PRE_ALARM_FIRE"
    const val ACTION_TASK_REMINDER_FIRE = "com.fritangui.wakeup.action.TASK_REMINDER_FIRE"
    const val ACTION_SKIP_NEXT_OCCURRENCE = "com.fritangui.wakeup.action.SKIP_NEXT_OCCURRENCE"
    const val ACTION_STOP_RINGING = "com.fritangui.wakeup.action.STOP_RINGING"
    const val ACTION_DISMISS_TASK_REMINDER = "com.fritangui.wakeup.action.DISMISS_TASK_REMINDER"
    const val ACTION_CLASS_REMINDER_FIRE = "com.fritangui.wakeup.action.CLASS_REMINDER_FIRE"
    const val ACTION_WIDGET_REFRESH_BOUNDARY = "com.fritangui.wakeup.action.WIDGET_REFRESH_BOUNDARY"

    const val NOTIF_ID_RINGING_BASE = 90_000
    const val NOTIF_ID_PRE_ALARM_BASE = 80_000
    const val NOTIF_ID_TASK_REMINDER_BASE = 70_000
    const val NOTIF_ID_REMINDER_BASE = 60_100
    const val NOTIF_ID_CLASS_REMINDER_BASE = 50_000

    /** Multiplicador para derivar request codes de PendingIntent únicos por alarma/propósito. */
    fun mainRequestCode(alarmId: Long): Int = (alarmId * 10 + 1).toInt()
    fun preAlarmRequestCode(alarmId: Long): Int = (alarmId * 10 + 2).toInt()
    fun showIntentRequestCode(alarmId: Long): Int = (alarmId * 10 + 3).toInt()
    fun taskReminderRequestCode(taskId: Long, reminderIndex: Int): Int =
        (taskId * 100 + reminderIndex).toInt()
    fun classReminderRequestCode(sessionId: Long): Int = (sessionId * 10 + 7).toInt()

    /** Un único PendingIntent global (no hay uno por sesión: siempre es "el próximo cruce, sea el que sea"). */
    const val REQUEST_CODE_WIDGET_REFRESH_BOUNDARY = 900_001
}
