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
    // Se subió a 6: se agregó `iconKey` a SubjectEntity (ícono elegido para reconocer la materia
    // más fácil). Misma razón que los saltos anteriores: ya hay datos reales del usuario, así que
    // migración explícita (MIGRATION_5_6).
    version = 6,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN description TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE tasks ADD COLUMN isNoteImportant INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE tasks ADD COLUMN gradeWeightPercent REAL")
                db.execSQL("ALTER TABLE tasks ADD COLUMN gradeValue REAL")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE subjects ADD COLUMN iconKey TEXT")
            }
        }
    }
}
