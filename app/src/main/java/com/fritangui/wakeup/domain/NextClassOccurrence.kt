package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

/** Una ocurrencia concreta (fecha real, no solo "los lunes") de una sesión de clase. */
data class UpcomingClassOccurrence(
    val subjectId: Long,
    val folderId: Long,
    val subjectName: String,
    val colorArgb: Int,
    val room: String,
    val start: LocalDateTime,
    val end: LocalDateTime,
)

/**
 * Calcula, para una lista de materias con sus sesiones semanales recurrentes,
 * las próximas [limit] ocurrencias concretas a partir de [now]. Es una función
 * pura (sin I/O) para poder testearla fácilmente y reutilizarla tanto en la
 * pantalla de inicio como en el widget de "próximas clases".
 */
fun computeNextClassOccurrences(
    subjects: List<SubjectWithSessions>,
    now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    limit: Int = 10,
): List<UpcomingClassOccurrence> {
    val today = now.date
    val occurrences = mutableListOf<UpcomingClassOccurrence>()

    for (entry in subjects) {
        for (session in entry.sessions) {
            // DayOfWeek.value en la JVM es ISO: 1=lunes .. 7=domingo, igual que ClassSessionEntity.dayOfWeek
            var daysUntil = session.dayOfWeek - today.dayOfWeek.value
            if (daysUntil < 0) daysUntil += 7
            var occurrenceDate = today.plus(daysUntil, DateTimeUnit.DAY)

            var start = occurrenceDate.atTime(session.startMinuteOfDay / 60, session.startMinuteOfDay % 60)
            if (daysUntil == 0 && start < now) {
                // Ya pasó hoy: la próxima es en 7 días
                occurrenceDate = occurrenceDate.plus(7, DateTimeUnit.DAY)
                start = occurrenceDate.atTime(session.startMinuteOfDay / 60, session.startMinuteOfDay % 60)
            }
            val end = occurrenceDate.atTime(session.endMinuteOfDay / 60, session.endMinuteOfDay % 60)

            occurrences += UpcomingClassOccurrence(
                subjectId = entry.subject.id,
                folderId = entry.subject.folderId,
                subjectName = entry.subject.name,
                colorArgb = entry.subject.colorArgb,
                room = session.room,
                start = start,
                end = end,
            )
        }
    }

    return occurrences.sortedBy { it.start }.take(limit)
}
