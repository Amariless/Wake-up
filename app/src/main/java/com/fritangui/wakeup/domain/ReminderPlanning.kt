package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * Calcula en qué instantes concretos hay que disparar los recordatorios de una
 * tarea, a partir de su fecha de vencimiento y sus offsets (p.ej. 7 días antes,
 * 1 día antes). Si la tarea no tiene fecha de vencimiento no hay nada que
 * programar. Los offsets que ya quedaron en el pasado respecto a [now] se
 * descartan (p.ej. si la tarea se crea 2 días antes de vencer, el recordatorio
 * de "1 semana antes" simplemente no se dispara).
 *
 * Además de los offsets configurables (los toggles de "1 semana antes"/"1 día antes" del editor),
 * SIEMPRE se agrega uno extra justo en el instante de vencimiento (#161: "que dé una noti extra
 * cuando se vence una tarea") — es independiente de esos toggles, así que avisa aunque los dos
 * estén apagados.
 */
fun computeReminderTriggers(
    task: TaskEntity,
    now: Instant = kotlinx.datetime.Clock.System.now(),
): List<Instant> {
    val dueAt = task.dueAtEpochMillis ?: return emptyList()
    val dueInstant = Instant.fromEpochMilliseconds(dueAt)
    val offsetTriggers = task.reminderOffsetsMinutes.map { offsetMinutes -> dueInstant.minus(offsetMinutes.minutes) }
    return (offsetTriggers + dueInstant)
        .distinct()
        .filter { it > now }
        .sorted()
}
