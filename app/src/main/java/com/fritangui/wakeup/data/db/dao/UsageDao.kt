package com.fritangui.wakeup.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.db.entity.BlockSurfaceUsageEntity
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDaily(entry: AppUsageDailyEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDailyAll(entries: List<AppUsageDailyEntity>)

    @Query("SELECT * FROM app_usage_daily WHERE dateEpochDay = :dateEpochDay ORDER BY minutesUsed DESC")
    fun observeForDay(dateEpochDay: Long): Flow<List<AppUsageDailyEntity>>

    @Query("SELECT * FROM app_usage_daily WHERE dateEpochDay BETWEEN :fromEpochDay AND :toEpochDay")
    fun observeForRange(fromEpochDay: Long, toEpochDay: Long): Flow<List<AppUsageDailyEntity>>

    @Query("SELECT COALESCE(SUM(minutesUsed), 0) FROM app_usage_daily WHERE dateEpochDay = :dateEpochDay AND packageName IN (:packageNames)")
    suspend fun sumMinutesForPackagesToday(dateEpochDay: Long, packageNames: List<String>): Long

    @Query("SELECT * FROM usage_alert_rules")
    fun observeAlertRules(): Flow<List<UsageAlertRuleEntity>>

    @Query("SELECT * FROM usage_alert_rules WHERE isEnabled = 1")
    suspend fun getEnabledAlertRules(): List<UsageAlertRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAlertRule(rule: UsageAlertRuleEntity): Long

    @Delete
    suspend fun deleteAlertRule(rule: UsageAlertRuleEntity)

    @Query("SELECT * FROM block_rules")
    fun observeBlockRules(): Flow<List<BlockRuleEntity>>

    @Query("SELECT * FROM block_rules WHERE isEnabled = 1")
    suspend fun getEnabledBlockRules(): List<BlockRuleEntity>

    @Query("SELECT * FROM block_rules WHERE packageName = :packageName AND isEnabled = 1")
    suspend fun getEnabledBlockRulesForPackage(packageName: String): List<BlockRuleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertBlockRule(rule: BlockRuleEntity): Long

    @Update
    suspend fun updateBlockRule(rule: BlockRuleEntity)

    @Delete
    suspend fun deleteBlockRule(rule: BlockRuleEntity)

    @Query("SELECT * FROM block_surface_usage WHERE dateEpochDay = :dateEpochDay AND surface = :surface")
    suspend fun getSurfaceUsage(dateEpochDay: Long, surface: BlockSurface): BlockSurfaceUsageEntity?

    @Query("SELECT * FROM block_surface_usage WHERE dateEpochDay = :dateEpochDay")
    fun observeSurfaceUsageForDay(dateEpochDay: Long): Flow<List<BlockSurfaceUsageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSurfaceUsage(usage: BlockSurfaceUsageEntity)
}
