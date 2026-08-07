package com.fritangui.wakeup.data.repository

import com.fritangui.wakeup.data.db.dao.AlarmDao
import com.fritangui.wakeup.data.db.dao.FolderDao
import com.fritangui.wakeup.data.db.entity.FolderEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FolderRepository @Inject constructor(
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

    /**
     * Marca la carpeta como terminada: deja de aparecer como activa (se archiva,
     * de solo lectura en la UI) y desactiva todas sus alarmas propias. La
     * cancelación real en AlarmManager y de los recordatorios de tareas la hace
     * el caso de uso [com.fritangui.wakeup.domain.usecase.TerminateFolder], que
     * también depende de [com.fritangui.wakeup.alarm.AlarmScheduler].
     */
    suspend fun markTerminated(id: Long) {
        folderDao.markTerminated(id)
        alarmDao.disableAllForFolder(id)
    }
}
