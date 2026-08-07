package com.fritangui.wakeup.data.repository

import com.fritangui.wakeup.data.db.dao.UsageDao
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.db.entity.BlockSurfaceUsageEntity
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepository @Inject constructor(
    private val usageDao: UsageDao,
) {
    fun observeForDay(dateEpochDay: Long): Flow<List<AppUsageDailyEntity>> = usageDao.observeForDay(dateEpochDay)
    fun observeForRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<AppUsageDailyEntity>> =
        usageDao.observeForRange(fromEpochDay, toEpochDay)

    suspend fun saveDailySnapshot(entries: List<AppUsageDailyEntity>) = usageDao.upsertDailyAll(entries)

    suspend fun minutesUsedTodayFor(dateEpochDay: Long, packageNames: List<String>): Long =
        usageDao.sumMinutesForPackagesToday(dateEpochDay, packageNames)

    fun observeAlertRules(): Flow<List<UsageAlertRuleEntity>> = usageDao.observeAlertRules()
    suspend fun getEnabledAlertRules(): List<UsageAlertRuleEntity> = usageDao.getEnabledAlertRules()
    suspend fun upsertAlertRule(rule: UsageAlertRuleEntity): Long = usageDao.upsertAlertRule(rule)
    suspend fun deleteAlertRule(rule: UsageAlertRuleEntity) = usageDao.deleteAlertRule(rule)

    fun observeBlockRules(): Flow<List<BlockRuleEntity>> = usageDao.observeBlockRules()
    suspend fun getEnabledBlockRules(): List<BlockRuleEntity> = usageDao.getEnabledBlockRules()
    suspend fun getEnabledBlockRulesForPackage(packageName: String): List<BlockRuleEntity> =
        usageDao.getEnabledBlockRulesForPackage(packageName)
    suspend fun upsertBlockRule(rule: BlockRuleEntity): Long = usageDao.upsertBlockRule(rule)
    suspend fun updateBlockRule(rule: BlockRuleEntity) = usageDao.updateBlockRule(rule)
    suspend fun deleteBlockRule(rule: BlockRuleEntity) = usageDao.deleteBlockRule(rule)

    suspend fun getSurfaceUsageMillis(dateEpochDay: Long, surface: BlockSurface): Long =
        usageDao.getSurfaceUsage(dateEpochDay, surface)?.accumulatedMillis ?: 0L

    fun observeSurfaceUsageForDay(dateEpochDay: Long): Flow<List<BlockSurfaceUsageEntity>> =
        usageDao.observeSurfaceUsageForDay(dateEpochDay)

    suspend fun addSurfaceUsageMillis(dateEpochDay: Long, surface: BlockSurface, deltaMillis: Long) {
        val current = getSurfaceUsageMillis(dateEpochDay, surface)
        usageDao.upsertSurfaceUsage(BlockSurfaceUsageEntity(dateEpochDay, surface, current + deltaMillis))
    }
}
