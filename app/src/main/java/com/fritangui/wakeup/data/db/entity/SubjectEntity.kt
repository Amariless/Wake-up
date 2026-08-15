package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una materia dentro de una [FolderEntity]. Sus horarios viven en [ClassSessionEntity]. */
@Entity(
    tableName = "subjects",
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderId")],
)
data class SubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val name: String,
    val colorArgb: Int,
    val professor: String = "",
    /** Clave del ícono elegido (ver [com.fritangui.wakeup.ui.subjects.SubjectIcons]); null = sin ícono, se muestra solo el punto de color. */
    val iconKey: String? = null,
)
