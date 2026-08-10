package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.entity.AlarmEntity
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atTime
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration

/**
 * Cálculo puro (sin AlarmManager, testeable) de la próxima vez que debe sonar
 * una alarma. Separado de [com.fritangui.wakeup.alarm.AlarmScheduler] para poder
 * probar la lógica de repetición de días sin depender del sistema operativo.
 */
object AlarmTiming {

    /**
     * @return el instante de la próxima ocurrencia, o null si la alarma está
     * deshabilitada, es de una sola vez con una fecha explícita ya pasada, o su
     * bitmask de repetición no tiene ningún día marcado.
     */
    fun nextTrigger(
        alarm: AlarmEntity,
        now: Instant = Clock.System.now(),
        zone: TimeZone = TimeZone.currentSystemDefault(),
    ): Instant? {
        if (!alarm.isEnabled) return null
        val nowLocal = now.toLocalDateTime(zone)

        return if (alarm.repeatDaysBitmask == 0) {
            nextOneShotTrigger(alarm, nowLocal, zone)
        } else {
            nextRepeatingTrigger(alarm, nowLocal, zone)
        }
    }

    private fun nextOneShotTrigger(alarm: AlarmEntity, nowLocal: LocalDateTime, zone: TimeZone): Instant? {
        val nowInstant = nowLocal.toInstant(zone)
        val date: LocalDate = alarm.oneShotDateEpochDay?.let { LocalDate.fromEpochDays(it.toInt()) }
            ?: nowLocal.date
        val trigger = date.atTime(alarm.hour, alarm.minute).toInstant(zone)
        if (trigger > nowInstant) return trigger

        // Sin fecha explícita y la hora ya pasó hoy: se entiende como "mañana a esa hora".
        if (alarm.oneShotDateEpochDay == null) {
            return nowLocal.date.plus(1, DateTimeUnit.DAY).atTime(alarm.hour, alarm.minute).toInstant(zone)
        }
        return null // fecha explícita ya pasada
    }

    private fun nextRepeatingTrigger(alarm: AlarmEntity, nowLocal: LocalDateTime, zone: TimeZone): Instant? {
        val nowInstant = nowLocal.toInstant(zone)
        for (daysAhead in 0..7) {
            val candidateDate = nowLocal.date.plus(daysAhead, DateTimeUnit.DAY)
            val isoDay = candidateDate.dayOfWeek.value
            if ((alarm.repeatDaysBitmask and AlarmEntity.dayBit(isoDay)) == 0) continue
            val candidate = candidateDate.atTime(alarm.hour, alarm.minute).toInstant(zone)
            if (candidate > nowInstant) return candidate
        }
        return null // bitmask sin días marcados
    }

    /**
     * Formatea una duración como "2d 4h 15m" (omitiendo las unidades más
     * grandes cuando son cero) para mostrar cuánto falta para que suene una
     * alarma, tanto en la lista como en el mensaje al guardar.
     */
    fun formatRemaining(remaining: Duration): String {
        if (remaining.isNegative()) return "en cualquier momento"
        val totalMinutes = remaining.inWholeMinutes
        val days = totalMinutes / (24 * 60)
        val hours = (totalMinutes % (24 * 60)) / 60
        val minutes = totalMinutes % 60
        return buildString {
            if (days > 0) append("${days}d ")
            if (days > 0 || hours > 0) append("${hours}h ")
            append("${minutes}m")
        }
    }
}
