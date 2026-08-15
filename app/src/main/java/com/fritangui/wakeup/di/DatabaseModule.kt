package com.fritangui.wakeup.di

import android.content.Context
import androidx.room.Room
import com.fritangui.wakeup.data.db.AppDatabase
import com.fritangui.wakeup.data.db.dao.AlarmDao
import com.fritangui.wakeup.data.db.dao.FolderDao
import com.fritangui.wakeup.data.db.dao.SubjectDao
import com.fritangui.wakeup.data.db.dao.TaskDao
import com.fritangui.wakeup.data.db.dao.UsageDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6)
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideFolderDao(db: AppDatabase): FolderDao = db.folderDao()

    @Provides
    fun provideSubjectDao(db: AppDatabase): SubjectDao = db.subjectDao()

    @Provides
    fun provideTaskDao(db: AppDatabase): TaskDao = db.taskDao()

    @Provides
    fun provideAlarmDao(db: AppDatabase): AlarmDao = db.alarmDao()

    @Provides
    fun provideUsageDao(db: AppDatabase): UsageDao = db.usageDao()
}
