package com.fritangui.wakeup.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE folderId = :folderId ORDER BY isCompleted, dueAtEpochMillis IS NULL, dueAtEpochMillis")
    fun observeByFolder(folderId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE subjectId = :subjectId ORDER BY isCompleted, dueAtEpochMillis IS NULL, dueAtEpochMillis")
    fun observeBySubject(subjectId: Long): Flow<List<TaskEntity>>

    @Query(
        "SELECT * FROM tasks WHERE isCompleted = 0 AND dueAtEpochMillis IS NOT NULL " +
            "AND folderId IN (SELECT id FROM folders WHERE isActive = 1) " +
            "ORDER BY dueAtEpochMillis LIMIT :limit",
    )
    fun observeUpcoming(limit: Int = 20): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun observeById(id: Long): Flow<TaskEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Query("UPDATE tasks SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("SELECT * FROM tasks WHERE folderId = :folderId")
    suspend fun getAllForFolder(folderId: Long): List<TaskEntity>
}
