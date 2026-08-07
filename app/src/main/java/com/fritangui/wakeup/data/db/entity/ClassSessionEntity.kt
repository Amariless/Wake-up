package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Un bloque horario de una materia. Una materia puede tener varias sesiones en
 * distintos días con distinto salón (p.ej. lunes en el salón 302 y miércoles en el
 * laboratorio 5), por eso es una tabla aparte y no columnas sueltas en Subject.
 *
 * [dayOfWeek] usa la convención ISO de kotlinx-datetime: 1 = lunes … 7 = domingo.
 */
@Entity(
    tableName = "class_sessions",
    foreignKeys = [
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId")],
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val dayOfWeek: Int,
    val startMinuteOfDay: Int,
    val endMinuteOfDay: Int,
    val room: String,
)
