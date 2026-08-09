package com.fritangui.wakeup.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    // Se subió a 3: se agregaron `kind` y `deleteAfterRing` a AlarmEntity (tipo "recordatorio" y
    // borrado automático tras sonar). A diferencia del salto 1→2, esta vez SÍ hay usuarios reales
    // con alarmas guardadas, así que se usa una migración explícita (MIGRATION_2_3) en vez de dejar
    // que fallbackToDestructiveMigration borre la tabla — ese fallback se deja solo como red de
    // seguridad para saltos de versión que no tengan una migración explícita.
    version = 3,
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

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN kind TEXT NOT NULL DEFAULT 'ALARM'")
                db.execSQL("ALTER TABLE alarms ADD COLUMN deleteAfterRing INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
