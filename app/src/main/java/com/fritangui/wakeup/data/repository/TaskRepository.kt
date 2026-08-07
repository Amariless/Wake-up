package com.fritangui.wakeup.data.repository

import com.fritangui.wakeup.data.db.dao.TaskDao
import com.fritangui.wakeup.data.db.entity.TaskEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TaskRepository @Inject constructor(
    private val taskDao: TaskDao,
) {
    fun observeByFolder(folderId: Long): Flow<List<TaskEntity>> = taskDao.observeByFolder(folderId)
    fun observeBySubject(subjectId: Long): Flow<List<TaskEntity>> = taskDao.observeBySubject(subjectId)
    fun observeUpcoming(limit: Int = 20): Flow<List<TaskEntity>> = taskDao.observeUpcoming(limit)
    fun observeById(id: Long): Flow<TaskEntity?> = taskDao.observeById(id)
    suspend fun getById(id: Long): TaskEntity? = taskDao.getById(id)
    suspend fun getAllForFolder(folderId: Long): List<TaskEntity> = taskDao.getAllForFolder(folderId)

    suspend fun upsert(task: TaskEntity): Long = taskDao.upsert(task)
    suspend fun setCompleted(id: Long, completed: Boolean) = taskDao.setCompleted(id, completed)
    suspend fun delete(task: TaskEntity) = taskDao.delete(task)
}
