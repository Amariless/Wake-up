package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.entity.AlarmEntity
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmTimingTest {

    private val zone = TimeZone.of("America/Bogota")

    // Miércoles 2026-08-05 08:00 hora local (arbitrario, dentro del rango del proyecto).
    private val wednesdayMorning: Instant = LocalDateTime(2026, 8, 5, 8, 0).toInstant(zone)

    @Test
    fun `alarma deshabilitada no tiene proxima ocurrencia`() {
        val alarm = baseAlarm(hour = 9, minute = 0, repeatDaysBitmask = 0b1111111, isEnabled = false)
        assertNull(AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone))
    }

    @Test
    fun `alarma repetitiva mas tarde hoy suena hoy mismo`() {
        val alarm = baseAlarm(hour = 9, minute = 0, repeatDaysBitmask = 0b1111111)
        val next = AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone)!!
        val nextLocal = next.toLocalDateTimeIn(zone)
        assertEquals(5, nextLocal.dayOfMonth) // mismo miércoles 5
        assertEquals(9, nextLocal.hour)
    }

    @Test
    fun `alarma repetitiva ya paso hoy salta a la proxima aparicion del bitmask`() {
        // Bitmask solo miércoles (día ISO 3): si ya pasó, la próxima es en 7 días.
        val alarm = baseAlarm(hour = 7, minute = 0, repeatDaysBitmask = AlarmEntity.dayBit(3))
        val next = AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone)!!
        val nextLocal = next.toLocalDateTimeIn(zone)
        assertEquals(12, nextLocal.dayOfMonth) // el miércoles siguiente
    }

    @Test
    fun `alarma repetitiva elige el dia mas cercano marcado en el bitmask`() {
        // Miércoles 08:00; marca lunes y viernes. El viernes de esta misma semana debe ganar.
        val bitmask = AlarmEntity.dayBit(1) or AlarmEntity.dayBit(5)
        val alarm = baseAlarm(hour = 10, minute = 0, repeatDaysBitmask = bitmask)
        val next = AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone)!!
        val nextLocal = next.toLocalDateTimeIn(zone)
        assertEquals(7, nextLocal.dayOfMonth) // viernes 7 de agosto de 2026
    }

    @Test
    fun `alarma de una sola vez sin fecha explicita cae manana si ya paso hoy`() {
        val alarm = baseAlarm(hour = 6, minute = 0, repeatDaysBitmask = 0, oneShotDateEpochDay = null)
        val next = AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone)!!
        val nextLocal = next.toLocalDateTimeIn(zone)
        assertEquals(6, nextLocal.dayOfMonth) // jueves 6
    }

    @Test
    fun `alarma de una sola vez con fecha explicita ya pasada no tiene proxima ocurrencia`() {
        val pastEpochDay = LocalDateTime(2026, 8, 4, 0, 0).date.toEpochDays().toLong() // martes 4 (ayer)
        val alarm = baseAlarm(hour = 9, minute = 0, repeatDaysBitmask = 0, oneShotDateEpochDay = pastEpochDay)
        assertNull(AlarmTiming.nextTrigger(alarm, wednesdayMorning, zone))
    }

    private fun baseAlarm(
        hour: Int,
        minute: Int,
        repeatDaysBitmask: Int,
        isEnabled: Boolean = true,
        oneShotDateEpochDay: Long? = null,
    ) = AlarmEntity(
        id = 1,
        folderId = null,
        label = "Test",
        hour = hour,
        minute = minute,
        repeatDaysBitmask = repeatDaysBitmask,
        oneShotDateEpochDay = oneShotDateEpochDay,
        isEnabled = isEnabled,
    )
}

private fun Instant.toLocalDateTimeIn(zone: TimeZone) = this.toLocalDateTime(zone)
