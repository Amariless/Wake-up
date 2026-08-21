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
            var end = occurrenceDate.atTime(session.endMinuteOfDay / 60, session.endMinuteOfDay % 60)
            // Antes se comparaba contra el INICIO ("start < now"): una clase que ya empezó pero
            // sigue en curso (start <= now < end) también cumplía esa condición y se empujaba a la
            // semana siguiente, haciendo que desapareciera del widget justo mientras estaba pasando.
            // Ahora solo se empuja si ya TERMINÓ hoy.
            if (daysUntil == 0 && end <= now) {
                occurrenceDate = occurrenceDate.plus(7, DateTimeUnit.DAY)
                start = occurrenceDate.atTime(session.startMinuteOfDay / 60, session.startMinuteOfDay % 60)
                end = occurrenceDate.atTime(session.endMinuteOfDay / 60, session.endMinuteOfDay % 60)
            }

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

/**
 * Próximo instante en que cambia qué se debería estar mostrando como "clase en curso" o "próxima
 * clase" — el fin de una clase que está pasando ahora mismo, o el inicio de la próxima ocurrencia
 * de cada sesión, lo que venga primero. Se usa para reprogramar el refresco de los widgets de home
 * screen justo en ese momento (#154), en vez de depender solo del refresco periódico de ~30 min
 * que impone Android en los widgets o de que el usuario haya cambiado algún dato mientras tanto.
 * Devuelve `null` si no hay ninguna sesión programada (nada que cruce nunca).
 */
fun nextWidgetRefreshBoundary(
    subjects: List<SubjectWithSessions>,
    now: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
): LocalDateTime? {
    // Un límite alto en vez del default de 10: acá se necesita el cruce de TODAS las sesiones, no
    // solo las próximas a mostrar en el widget de "próximas clases".
    val occurrences = computeNextClassOccurrences(subjects, now, limit = Int.MAX_VALUE)
    var earliest: LocalDateTime? = null
    for (occurrence in occurrences) {
        val boundary = if (occurrence.start <= now && occurrence.end > now) occurrence.end else occurrence.start
        if (earliest == null || boundary < earliest) earliest = boundary
    }
    return earliest
}
