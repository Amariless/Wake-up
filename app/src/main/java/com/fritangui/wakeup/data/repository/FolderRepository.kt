package com.fritangui.wakeup.data.repository

import androidx.room.withTransaction
import com.fritangui.wakeup.data.db.AppDatabase
import com.fritangui.wakeup.data.db.dao.AlarmDao
import com.fritangui.wakeup.data.db.dao.FolderDao
import com.fritangui.wakeup.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
    private val db: AppDatabase,
    private val folderDao: FolderDao,
    private val alarmDao: AlarmDao,
) {
    fun observeAll(): Flow<List<FolderEntity>> = folderDao.observeAll()
    fun observeActive(): Flow<List<FolderEntity>> = folderDao.observeActive()
    fun observeById(id: Long): Flow<FolderEntity?> = folderDao.observeById(id)
    suspend fun getById(id: Long): FolderEntity? = folderDao.getById(id)

    suspend fun create(name: String, colorArgb: Int): Long =
        folderDao.upsert(FolderEntity(name = name, colorArgb = colorArgb))

    suspend fun rename(folder: FolderEntity, newName: String, newColorArgb: Int) {
        folderDao.update(folder.copy(name = newName, colorArgb = newColorArgb))
    }

    suspend fun reactivate(id: Long) = folderDao.reactivate(id)

    suspend fun delete(folder: FolderEntity) = folderDao.delete(folder)

    /** Borra por nombre exacto (con todo lo que cuelgue de ella vía cascada). Ver DevTools' seedDemoData. */
    suspend fun deleteByExactName(name: String) = folderDao.deleteByName(name)

    /**
     * Marca la carpeta como terminada: deja de aparecer como activa (se archiva,
     * de solo lectura en la UI) y desactiva todas sus alarmas propias. La
     * cancelación real en AlarmManager y de los recordatorios de tareas la hace
     * [com.fritangui.wakeup.alarm.AlarmController.terminateFolder], que llama a esto.
     * Las dos escrituras van en una sola transacción: si el proceso muere entre
     * medio, no puede quedar la carpeta archivada con sus alarmas todavía encendidas.
     */
    suspend fun markTerminated(id: Long) = db.withTransaction {
        folderDao.markTerminated(id)
        alarmDao.disableAllForFolder(id)
    }
}
