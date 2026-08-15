package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.minutes

/**
 * Cálculo puro (sin AlarmManager, testeable) de la próxima vez que hay que avisar "tu clase de tal
 * empieza en X min" para una sesión recurrente — mismo espíritu que [AlarmTiming.nextTrigger], pero
 * para el aviso global de próxima clase (#144).
 *
 * @return null si [minutesBefore] es 0 o menos (aviso apagado), o si por algún motivo no se
 * encuentra ninguna ocurrencia futura en los próximos 7 días (no debería pasar: toda sesión ocurre
 * una vez por semana).
 */
fun nextClassReminderTrigger(
    session: ClassSessionEntity,
    minutesBefore: Int,
    now: Instant = Clock.System.now(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Instant? {
    if (minutesBefore <= 0) return null
    val nowLocal = now.toLocalDateTime(zone)
    for (daysAhead in 0..7) {
        val candidateDate = nowLocal.date.plus(daysAhead, DateTimeUnit.DAY)
        if (candidateDate.dayOfWeek.value != session.dayOfWeek) continue
        val classStart = candidateDate
            .atTime(session.startMinuteOfDay / 60, session.startMinuteOfDay % 60)
            .toInstant(zone)
        val trigger = classStart.minus(minutesBefore.minutes)
        if (trigger > now) return trigger
    }
    return null
}
