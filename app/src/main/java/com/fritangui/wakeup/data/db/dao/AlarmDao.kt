package com.fritangui.wakeup.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AlarmDao {
    /** Alarmas del reloj general (no atadas a ninguna carpeta), más recientemente usada/creada primero. */
    @Query("SELECT * FROM alarms WHERE folderId IS NULL ORDER BY COALESCE(lastTriggeredAtEpochMillis, createdAtEpochMillis) DESC")
    fun observeGeneralAlarms(): Flow<List<AlarmEntity>>

    @Query("SELECT * FROM alarms WHERE folderId = :folderId ORDER BY COALESCE(lastTriggeredAtEpochMillis, createdAtEpochMillis) DESC")
    fun observeByFolder(folderId: Long): Flow<List<AlarmEntity>>

    @Query(
        "SELECT * FROM alarms WHERE isEnabled = 1 AND (folderId IS NULL OR folderId IN (SELECT id FROM folders WHERE isActive = 1))",
    )
    suspend fun getAllActiveEnabled(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE id = :id")
    suspend fun getById(id: Long): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE id = :id")
    fun observeById(id: Long): Flow<AlarmEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(alarm: AlarmEntity): Long

    @Update
    suspend fun update(alarm: AlarmEntity)

    @Query("UPDATE alarms SET isEnabled = :enabled WHERE id = :id")
    suspend fun setEnabled(id: Long, enabled: Boolean)

    @Query("UPDATE alarms SET skipNextOccurrence = :skip WHERE id = :id")
    suspend fun setSkipNext(id: Long, skip: Boolean)

    @Query("UPDATE alarms SET lastTriggeredAtEpochMillis = :atEpochMillis WHERE id = :id")
    suspend fun setLastTriggered(id: Long, atEpochMillis: Long)

    @Query("UPDATE alarms SET isEnabled = 0 WHERE folderId = :folderId")
    suspend fun disableAllForFolder(folderId: Long)

    @Delete
    suspend fun delete(alarm: AlarmEntity)

    @Query("SELECT * FROM alarms WHERE folderId = :folderId")
    suspend fun getAllForFolder(folderId: Long): List<AlarmEntity>
}
