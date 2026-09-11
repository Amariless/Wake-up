package com.fritangui.wakeup.domain

/**
 * En qué momento vence una tarea respecto a ahora — usado para separar "Próximas tareas" en
 * grupos en vez de una sola lista plana larga (#161). Los nombres son para el propio código, no
 * lo que ve el usuario (eso lo decide cada pantalla con su propio texto).
 */
enum class TaskUrgencyBucket {
    OVERDUE,
    WITHIN_3_DAYS,
    WITHIN_1_WEEK,
    LATER,
    NO_DUE_DATE,
}

private const val THREE_DAYS_MILLIS = 3 * 24 * 60 * 60 * 1000L
private const val SEVEN_DAYS_MILLIS = 7 * 24 * 60 * 60 * 1000L

/** Función pura (testeable) que clasifica una fecha de vencimiento contra [nowEpochMillis]. */
fun taskUrgencyBucket(dueAtEpochMillis: Long?, nowEpochMillis: Long): TaskUrgencyBucket {
    if (dueAtEpochMillis == null) return TaskUrgencyBucket.NO_DUE_DATE
    val remaining = dueAtEpochMillis - nowEpochMillis
    return when {
        remaining < 0 -> TaskUrgencyBucket.OVERDUE
        remaining < THREE_DAYS_MILLIS -> TaskUrgencyBucket.WITHIN_3_DAYS
        remaining < SEVEN_DAYS_MILLIS -> TaskUrgencyBucket.WITHIN_1_WEEK
        else -> TaskUrgencyBucket.LATER
    }
}
