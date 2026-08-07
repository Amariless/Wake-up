package com.fritangui.wakeup.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Una "carpeta" de organización: normalmente un semestre, pero el usuario puede
 * usarla para cualquier otra agrupación (un curso libre, un bootcamp, etc.).
 * Al "terminar" una carpeta se pone [isActive] = false, lo que cancela todas las
 * alarmas y recordatorios asociados a sus materias/tareas/alarmas sin borrar el
 * historial (ver TerminateFolder use-case).
 */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val colorArgb: Int,
    val isActive: Boolean = true,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
)
