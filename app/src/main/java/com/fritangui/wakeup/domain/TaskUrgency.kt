package com.fritangui.wakeup.domain

import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/**
 * En qué momento vence una tarea respecto a ahora — usado para separar "Próximas tareas" en
 * grupos en vez de una sola lista plana larga (#161). Los nombres son para el propio código, no
 * lo que ve el usuario (eso lo decide [taskUrgencyBucketLabel], compartida por Inicio y el widget
 * para que los dos muestren exactamente los mismos subtítulos).
 */
enum class TaskUrgencyBucket {
    OVERDUE,
    TODAY,
    TOMORROW,
    WITHIN_3_DAYS,
    WITHIN_1_WEEK,
    LATER,
    NO_DUE_DATE,
}

/**
 * Función pura (testeable) que clasifica una fecha de vencimiento contra [nowEpochMillis], por
 * DÍA DE CALENDARIO (no por milisegundos restantes, #161: "hoy"/"mañana" tienen que coincidir con
 * el día real, no con una ventana móvil de 24h/72h desde el instante actual — una tarea que vence
 * hoy a las 9am sigue siendo "Hoy" a las 5pm, no pasa a "Vencida" hasta que cambia el día).
 */
fun taskUrgencyBucket(
    dueAtEpochMillis: Long?,
    nowEpochMillis: Long,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
): TaskUrgencyBucket {
    if (dueAtEpochMillis == null) return TaskUrgencyBucket.NO_DUE_DATE
    val today = Instant.fromEpochMilliseconds(nowEpochMillis).toLocalDateTime(timeZone).date
    val dueDate = Instant.fromEpochMilliseconds(dueAtEpochMillis).toLocalDateTime(timeZone).date
    val diffDays = dueDate.toEpochDays() - today.toEpochDays()
    return when {
        diffDays < 0 -> TaskUrgencyBucket.OVERDUE
        diffDays == 0 -> TaskUrgencyBucket.TODAY
        diffDays == 1 -> TaskUrgencyBucket.TOMORROW
        diffDays <= 3 -> TaskUrgencyBucket.WITHIN_3_DAYS
        diffDays <= 6 -> TaskUrgencyBucket.WITHIN_1_WEEK
        else -> TaskUrgencyBucket.LATER
    }
}

/** Subtítulo que ve el usuario para cada grupo — compartido por Inicio (Compose) y el widget
 *  (Glance) para que digan siempre lo mismo (#161: "en 'próximas tareas' (widget e inicio)"). */
fun taskUrgencyBucketLabel(bucket: TaskUrgencyBucket): String = when (bucket) {
    TaskUrgencyBucket.OVERDUE -> "Vencidas"
    TaskUrgencyBucket.TODAY -> "Hoy"
    TaskUrgencyBucket.TOMORROW -> "Mañana"
    TaskUrgencyBucket.WITHIN_3_DAYS -> "Próximos días"
    TaskUrgencyBucket.WITHIN_1_WEEK -> "Esta semana"
    TaskUrgencyBucket.LATER -> "Más adelante"
    TaskUrgencyBucket.NO_DUE_DATE -> "Sin fecha"
}
