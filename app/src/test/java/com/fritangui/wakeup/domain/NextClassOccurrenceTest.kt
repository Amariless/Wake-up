package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import kotlinx.datetime.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Test

class NextClassOccurrenceTest {

    // Miércoles 2026-08-05, 08:00.
    private val wednesdayMorning = LocalDateTime(2026, 8, 5, 8, 0)

    private fun subjectWith(vararg sessions: ClassSessionEntity, subjectId: Long = 1, name: String = "Cálculo I") =
        SubjectWithSessions(
            subject = SubjectEntity(id = subjectId, folderId = 1, name = name, colorArgb = 0xFF000000.toInt()),
            sessions = sessions.toList(),
        )

    @Test
    fun `sesion mas tarde el mismo dia aparece primero`() {
        val entry = subjectWith(
            ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 3, startMinuteOfDay = 9 * 60, endMinuteOfDay = 11 * 60, room = "302"),
        )
        val result = computeNextClassOccurrences(listOf(entry), wednesdayMorning)
        assertEquals(1, result.size)
        assertEquals(5, result.first().start.dayOfMonth) // mismo miércoles
        assertEquals(9, result.first().start.hour)
    }

    @Test
    fun `sesion que ya paso hoy salta a la proxima semana`() {
        val entry = subjectWith(
            ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 3, startMinuteOfDay = 6 * 60, endMinuteOfDay = 8 * 60, room = "302"),
        )
        val result = computeNextClassOccurrences(listOf(entry), wednesdayMorning)
        assertEquals(12, result.first().start.dayOfMonth) // el miércoles siguiente
    }

    @Test
    fun `las ocurrencias quedan ordenadas cronologicamente entre varias materias`() {
        val calculo = subjectWith(
            subjectId = 1,
            name = "Cálculo I",
            sessions = arrayOf(
                ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 5, startMinuteOfDay = 8 * 60, endMinuteOfDay = 10 * 60, room = "302"),
            ),
        )
        val fisica = subjectWith(
            subjectId = 2,
            name = "Física I",
            sessions = arrayOf(
                ClassSessionEntity(id = 2, subjectId = 2, dayOfWeek = 3, startMinuteOfDay = 14 * 60, endMinuteOfDay = 16 * 60, room = "Lab 2"),
            ),
        )
        val result = computeNextClassOccurrences(listOf(calculo, fisica), wednesdayMorning)
        assertEquals("Física I", result.first().subjectName) // hoy en la tarde, antes que el viernes
        assertEquals("Cálculo I", result[1].subjectName)
    }

    @Test
    fun `respeta el limite pedido`() {
        val entry = subjectWith(
            *(1..7).map { day ->
                ClassSessionEntity(id = day.toLong(), subjectId = 1, dayOfWeek = day, startMinuteOfDay = 8 * 60, endMinuteOfDay = 9 * 60, room = "302")
            }.toTypedArray(),
        )
        val result = computeNextClassOccurrences(listOf(entry), wednesdayMorning, limit = 3)
        assertEquals(3, result.size)
    }

    @Test
    fun `el proximo cruce es el inicio de una clase que todavia no empieza`() {
        val entry = subjectWith(
            ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 3, startMinuteOfDay = 9 * 60, endMinuteOfDay = 11 * 60, room = "302"),
        )
        val boundary = nextWidgetRefreshBoundary(listOf(entry), wednesdayMorning)
        assertEquals(LocalDateTime(2026, 8, 5, 9, 0), boundary)
    }

    @Test
    fun `el proximo cruce es el fin de una clase en curso, no su siguiente inicio`() {
        val entry = subjectWith(
            ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 3, startMinuteOfDay = 7 * 60, endMinuteOfDay = 9 * 60, room = "302"),
        )
        // wednesdayMorning = miércoles 08:00, adentro de la sesión 07:00-09:00.
        val boundary = nextWidgetRefreshBoundary(listOf(entry), wednesdayMorning)
        assertEquals(LocalDateTime(2026, 8, 5, 9, 0), boundary) // el fin de esta clase, no el miércoles siguiente
    }

    @Test
    fun `el proximo cruce es el mas cercano entre varias materias`() {
        val enCurso = subjectWith(
            subjectId = 1,
            sessions = arrayOf(
                ClassSessionEntity(id = 1, subjectId = 1, dayOfWeek = 3, startMinuteOfDay = 7 * 60, endMinuteOfDay = 8 * 60 + 30, room = "302"),
            ),
        )
        val masTarde = subjectWith(
            subjectId = 2,
            sessions = arrayOf(
                ClassSessionEntity(id = 2, subjectId = 2, dayOfWeek = 3, startMinuteOfDay = 14 * 60, endMinuteOfDay = 16 * 60, room = "Lab 2"),
            ),
        )
        val boundary = nextWidgetRefreshBoundary(listOf(enCurso, masTarde), wednesdayMorning)
        assertEquals(LocalDateTime(2026, 8, 5, 8, 30), boundary) // el fin de la que está en curso, no el inicio de la otra
    }

    @Test
    fun `sin ninguna sesion no hay proximo cruce`() {
        val boundary = nextWidgetRefreshBoundary(emptyList(), wednesdayMorning)
        assertEquals(null, boundary)
    }
}
