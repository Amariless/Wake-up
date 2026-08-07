package com.fritangui.wakeup.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Update
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow

/** Una materia junto con todas sus sesiones/horarios (posiblemente en distintos salones). */
data class SubjectWithSessions(
    @Embedded val subject: SubjectEntity,
    @Relation(parentColumn = "id", entityColumn = "subjectId")
    val sessions: List<ClassSessionEntity>,
)

@Dao
interface SubjectDao {
    @Query("SELECT * FROM subjects WHERE folderId = :folderId ORDER BY name")
    fun observeByFolder(folderId: Long): Flow<List<SubjectEntity>>

    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeById(id: Long): Flow<SubjectEntity?>

    @androidx.room.Transaction
    @Query("SELECT * FROM subjects WHERE folderId = :folderId ORDER BY name")
    fun observeWithSessionsByFolder(folderId: Long): Flow<List<SubjectWithSessions>>

    @androidx.room.Transaction
    @Query("SELECT * FROM subjects WHERE id = :id")
    fun observeWithSessionsById(id: Long): Flow<SubjectWithSessions?>

    @androidx.room.Transaction
    @Query("SELECT * FROM subjects WHERE folderId IN (SELECT id FROM folders WHERE isActive = 1)")
    fun observeWithSessionsForActiveFolders(): Flow<List<SubjectWithSessions>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(subject: SubjectEntity): Long

    @Update
    suspend fun update(subject: SubjectEntity)

    @Delete
    suspend fun delete(subject: SubjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSession(session: ClassSessionEntity): Long

    @Delete
    suspend fun deleteSession(session: ClassSessionEntity)

    @Query("SELECT * FROM class_sessions WHERE subjectId = :subjectId ORDER BY dayOfWeek, startMinuteOfDay")
    fun observeSessions(subjectId: Long): Flow<List<ClassSessionEntity>>
}
