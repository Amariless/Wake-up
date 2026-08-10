package com.fritangui.wakeup.widget

import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.data.repository.UsageRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import android.content.Context

/**
 * Los `GlanceAppWidget` no son components de Android estándar (no son
 * Activity/Service/Receiver/View), así que Hilt no los puede inyectar con
 * `@AndroidEntryPoint`. Este EntryPoint permite obtener los repositorios a
 * mano desde `provideGlance`.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WidgetEntryPoint {
    fun subjectRepository(): SubjectRepository
    fun taskRepository(): TaskRepository
    fun usageRepository(): UsageRepository
}

fun widgetEntryPoint(context: Context): WidgetEntryPoint =
    EntryPointAccessors.fromApplication(context.applicationContext, WidgetEntryPoint::class.java)
