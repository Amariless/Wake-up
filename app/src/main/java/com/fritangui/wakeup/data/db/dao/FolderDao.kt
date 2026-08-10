package com.fritangui.wakeup.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fritangui.wakeup.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY isActive DESC, createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE isActive = 1 ORDER BY createdAtEpochMillis DESC")
    fun observeActive(): Flow<List<FolderEntity>>

    @Query("SELECT * FROM folders WHERE id = :id")
    fun observeById(id: Long): Flow<FolderEntity?>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): FolderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(folder: FolderEntity): Long

    @Update
    suspend fun update(folder: FolderEntity)

    @Query("UPDATE folders SET isActive = 0 WHERE id = :id")
    suspend fun markTerminated(id: Long)

    @Query("UPDATE folders SET isActive = 1 WHERE id = :id")
    suspend fun reactivate(id: Long)

    @Delete
    suspend fun delete(folder: FolderEntity)

    /** Para limpieza puntual (p.ej. borrar datos de ejemplo por nombre exacto). */
    @Query("DELETE FROM folders WHERE name = :name")
    suspend fun deleteByName(name: String)
}
