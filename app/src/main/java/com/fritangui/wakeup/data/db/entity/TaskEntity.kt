package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tarea asociada a una carpeta (y opcionalmente a una materia concreta).
 * [dueAtEpochMillis] es nullable a propósito: el usuario puede dejar la tarea
 * sin fecha de vencimiento. [reminderOffsetsMinutes] son los minutos de
 * anticipación con los que se debe avisar (por defecto 1 semana + 1 día);
 * si la tarea no tiene fecha, no se programa ningún recordatorio.
 */
@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = SubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("folderId"), Index("subjectId")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val subjectId: Long?,
    val title: String,
    /** Detalle largo de la tarea (multilínea); distinto de [notes], que es más una nota corta. */
    val description: String = "",
    val notes: String = "",
    /** Si está marcada, [notes] también se muestra como línea extra debajo de esta tarea en el widget. */
    val isNoteImportant: Boolean = false,
    val dueAtEpochMillis: Long?,
    val reminderOffsetsMinutes: List<Long> = DEFAULT_REMINDER_OFFSETS,
    val isCompleted: Boolean = false,
    /** Cuánto vale esta tarea en la nota final (p.ej. 15.0 = 15%). Se puede fijar desde que se crea
     *  la tarea, o dejarlo para más tarde y confirmarlo/editarlo al marcarla como completada. */
    val gradeWeightPercent: Double? = null,
    /** La nota obtenida en esta tarea; solo tiene sentido una vez completada. */
    val gradeValue: Double? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
) {
    companion object {
        /** 1 semana antes + 1 día antes, el valor por defecto pedido. */
        val DEFAULT_REMINDER_OFFSETS = listOf(7 * 24 * 60L, 24 * 60L)
    }
}
