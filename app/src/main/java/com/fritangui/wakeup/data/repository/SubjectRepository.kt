package com.fritangui.wakeup.data.repository

import com.fritangui.wakeup.data.db.dao.SubjectDao
import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubjectRepository @Inject constructor(
    private val subjectDao: SubjectDao,
) {
    fun observeByFolder(folderId: Long): Flow<List<SubjectEntity>> = subjectDao.observeByFolder(folderId)
    fun observeById(id: Long): Flow<SubjectEntity?> = subjectDao.observeById(id)
    fun observeWithSessionsByFolder(folderId: Long): Flow<List<SubjectWithSessions>> =
        subjectDao.observeWithSessionsByFolder(folderId)
    fun observeWithSessionsById(id: Long): Flow<SubjectWithSessions?> = subjectDao.observeWithSessionsById(id)
    fun observeWithSessionsForActiveFolders(): Flow<List<SubjectWithSessions>> =
        subjectDao.observeWithSessionsForActiveFolders()
    fun observeSessions(subjectId: Long): Flow<List<ClassSessionEntity>> = subjectDao.observeSessions(subjectId)

    suspend fun upsert(subject: SubjectEntity): Long = subjectDao.upsert(subject)
    suspend fun update(subject: SubjectEntity) = subjectDao.update(subject)
    suspend fun delete(subject: SubjectEntity) = subjectDao.delete(subject)

    suspend fun upsertSession(session: ClassSessionEntity): Long = subjectDao.upsertSession(session)
    suspend fun deleteSession(session: ClassSessionEntity) = subjectDao.deleteSession(session)
}
