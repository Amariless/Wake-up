package com.fritangui.wakeup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.fritangui.wakeup.data.db.dao.AlarmDao
import com.fritangui.wakeup.data.db.dao.FolderDao
import com.fritangui.wakeup.data.db.dao.SubjectDao
import com.fritangui.wakeup.data.db.dao.TaskDao
import com.fritangui.wakeup.data.db.dao.UsageDao
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurfaceUsageEntity
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity

@Database(
    entities = [
        FolderEntity::class,
        SubjectEntity::class,
        ClassSessionEntity::class,
        TaskEntity::class,
        AlarmEntity::class,
        AppUsageDailyEntity::class,
        UsageAlertRuleEntity::class,
        BlockRuleEntity::class,
        BlockSurfaceUsageEntity::class,
    ],
    // Se subió a 2 porque se agregaron columnas a AlarmEntity (createdAtEpochMillis,
    // lastTriggeredAtEpochMillis) y la tabla block_surface_usage sin bumpear la versión —
    // eso rompía la verificación de integridad de Room y crasheaba la app al abrir en
    // cualquier instalación que ya tuviera la base de datos vieja creada. No hay migración
    // explícita porque el proyecto aún no tiene datos de usuarios reales que preservar
    // (fallbackToDestructiveMigration recrea las tablas, ver DatabaseModule).
    version = 2,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun subjectDao(): SubjectDao
    abstract fun taskDao(): TaskDao
    abstract fun alarmDao(): AlarmDao
    abstract fun usageDao(): UsageDao

    companion object {
        const val DATABASE_NAME = "wakeup.db"
    }
}
