package com.fritangui.wakeup.data.repository

import com.fritangui.wakeup.data.db.dao.AlarmDao
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Acceso puro a la tabla de alarmas (Room). No programa nada en AlarmManager:
 * eso lo hace [com.fritangui.wakeup.alarm.AlarmController], que combina este
 * repositorio con [com.fritangui.wakeup.alarm.AlarmScheduler] para que crear/editar/
 * borrar una alarma también actualice el sistema operativo en el mismo paso.
 */
@Singleton
class AlarmRepository @Inject constructor(
    private val alarmDao: AlarmDao,
) {
    fun observeGeneralAlarms(): Flow<List<AlarmEntity>> = alarmDao.observeGeneralAlarms()
    fun observeByFolder(folderId: Long): Flow<List<AlarmEntity>> = alarmDao.observeByFolder(folderId)
    fun observeById(id: Long): Flow<AlarmEntity?> = alarmDao.observeById(id)
    suspend fun getById(id: Long): AlarmEntity? = alarmDao.getById(id)
    suspend fun getAllActiveEnabled(): List<AlarmEntity> = alarmDao.getAllActiveEnabled()
    suspend fun getAllForFolder(folderId: Long): List<AlarmEntity> = alarmDao.getAllForFolder(folderId)

    suspend fun upsert(alarm: AlarmEntity): Long = alarmDao.upsert(alarm)
    suspend fun update(alarm: AlarmEntity) = alarmDao.update(alarm)
    suspend fun setEnabled(id: Long, enabled: Boolean) = alarmDao.setEnabled(id, enabled)
    suspend fun setSkipNext(id: Long, skip: Boolean) = alarmDao.setSkipNext(id, skip)
    suspend fun disableAllForFolder(folderId: Long) = alarmDao.disableAllForFolder(folderId)
    suspend fun delete(alarm: AlarmEntity) = alarmDao.delete(alarm)
}
