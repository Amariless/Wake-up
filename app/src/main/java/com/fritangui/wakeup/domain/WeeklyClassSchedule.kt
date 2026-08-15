package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.dao.SubjectWithSessions

/** Una clase concreta dentro de un día de la semana (horario recurrente, no una fecha puntual). */
data class WeeklyClassEntry(
    val subjectId: Long,
    val folderId: Long,
    val subjectName: String,
    val colorArgb: Int,
    val room: String,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
)

/** [dayOfWeek] es ISO: 1=lunes .. 7=domingo, igual que [com.fritangui.wakeup.data.db.entity.ClassSessionEntity.dayOfWeek]. */
data class WeeklyClassDay(
    val dayOfWeek: Int,
    val classes: List<WeeklyClassEntry>,
)

/**
 * A diferencia de [computeNextClassOccurrences] (próximas N ocurrencias concretas, puede repetir
 * varias veces el mismo día si hay varias clases seguidas, o saltarse días completos si ya se
 * llenó el límite), esto arma una semana completa de lunes a domingo con el horario RECURRENTE de
 * cada materia — pensado para el calendario semanal de Inicio (#140), que salta los días sin
 * ninguna clase en vez de mostrarlos vacíos.
 */
fun computeWeeklyClassSchedule(subjects: List<SubjectWithSessions>): List<WeeklyClassDay> =
    (1..7).map { day ->
        val classes = subjects.flatMap { entry ->
            entry.sessions.filter { it.dayOfWeek == day }.map { session ->
                WeeklyClassEntry(
                    subjectId = entry.subject.id,
                    folderId = entry.subject.folderId,
                    subjectName = entry.subject.name,
                    colorArgb = entry.subject.colorArgb,
                    room = session.room,
                    startMinuteOfDay = session.startMinuteOfDay,
                    endMinuteOfDay = session.endMinuteOfDay,
                )
            }
        }.sortedBy { it.startMinuteOfDay }
        WeeklyClassDay(day, classes)
    }.filter { it.classes.isNotEmpty() }

/** El próximo día (1=lunes..7=domingo) con al menos una clase, empezando a buscar desde [todayDayOfWeek] inclusive. */
fun nextClassDayOfWeek(weekly: List<WeeklyClassDay>, todayDayOfWeek: Int): Int? {
    if (weekly.isEmpty()) return null
    val daysWithClasses = weekly.map { it.dayOfWeek }.toSet()
    for (offset in 0..6) {
        val candidate = ((todayDayOfWeek - 1 + offset) % 7) + 1
        if (candidate in daysWithClasses) return candidate
    }
    return null
}
