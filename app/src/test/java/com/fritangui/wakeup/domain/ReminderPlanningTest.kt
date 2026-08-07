package com.fritangui.wakeup.domain

import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReminderPlanningTest {

    private val now = Instant.fromEpochMilliseconds(1_754_380_800_000L) // 2025-08-05 08:00 UTC aprox.

    @Test
    fun `tarea sin fecha de vencimiento no genera recordatorios`() {
        val task = TaskEntity(id = 1, folderId = 1, subjectId = null, title = "Sin fecha", dueAtEpochMillis = null)
        assertTrue(computeReminderTriggers(task, now).isEmpty())
    }

    @Test
    fun `los offsets por defecto generan dos recordatorios futuros`() {
        val dueInTwoWeeks = now.plus(kotlin.time.Duration.parse("14d")).toEpochMilliseconds()
        val task = TaskEntity(id = 1, folderId = 1, subjectId = null, title = "Entrega", dueAtEpochMillis = dueInTwoWeeks)
        val triggers = computeReminderTriggers(task, now)
        assertEquals(2, triggers.size)
        assertTrue(triggers.all { it > now })
    }

    @Test
    fun `offsets que ya quedaron en el pasado se descartan`() {
        // La tarea vence en 25h: el recordatorio de "1 semana antes" ya pasó, solo queda "1 día antes"
        // (usamos 25h en vez de exactamente 1 día para que ese offset caiga claramente en el futuro y no justo en "now").
        val dueSoon = now.plus(kotlin.time.Duration.parse("25h")).toEpochMilliseconds()
        val task = TaskEntity(id = 1, folderId = 1, subjectId = null, title = "Entrega urgente", dueAtEpochMillis = dueSoon)
        val triggers = computeReminderTriggers(task, now)
        assertEquals(1, triggers.size)
    }

    @Test
    fun `los recordatorios quedan ordenados de mas lejano a mas cercano`() {
        val dueInThreeWeeks = now.plus(kotlin.time.Duration.parse("21d")).toEpochMilliseconds()
        val task = TaskEntity(id = 1, folderId = 1, subjectId = null, title = "Proyecto final", dueAtEpochMillis = dueInThreeWeeks)
        val triggers = computeReminderTriggers(task, now)
        assertEquals(triggers, triggers.sorted())
    }
}
