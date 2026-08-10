package com.fritangui.wakeup.ui.devtools

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmConstants
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.alarm.RingingForegroundService
import com.fritangui.wakeup.blocking.BlockOverlayService
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.random.Random

/**
 * Solo se instancia en builds debug (ver [com.fritangui.wakeup.BuildConfig.DEV_TOOLS_ENABLED]).
 * Atajos para poder probar todo el flujo de alarmas/bloqueo/uso desde el
 * emulador sin tener que esperar horarios reales.
 */
@HiltViewModel
class DevToolsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val folderRepository: FolderRepository,
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val alarmRepository: AlarmRepository,
    private val alarmController: AlarmController,
    private val usageRepository: UsageRepository,
    private val notificationHelper: NotificationHelper,
) : ViewModel() {

    fun fireAlarmNow() {
        viewModelScope.launch {
            val id = alarmRepository.upsert(
                AlarmEntity(
                    folderId = null,
                    label = "Prueba (dev tools)",
                    hour = 0,
                    minute = 0,
                    dismissChallenge = DismissChallengeType.MATH_PROBLEM,
                ),
            )
            val serviceIntent = Intent(context, RingingForegroundService::class.java).apply {
                action = RingingForegroundService.ACTION_START_RINGING
                putExtra(AlarmConstants.EXTRA_ALARM_ID, id)
            }
            ContextCompat.startForegroundService(context, serviceIntent)
        }
    }

    fun simulatePreAlarmNotification() {
        viewModelScope.launch {
            val id = alarmRepository.upsert(
                AlarmEntity(folderId = null, label = "Prueba T-60 (dev tools)", hour = 7, minute = 0),
            )
            val alarm = alarmRepository.getById(id) ?: return@launch
            notificationHelper.notifyPreAlarm(alarm)
        }
    }

    fun seedDemoData() {
        viewModelScope.launch {
            val folderId = folderRepository.create("Demo 2026-1", 0xFF3D5AFE.toInt())
            val subjectId = subjectRepository.upsert(
                com.fritangui.wakeup.data.db.entity.SubjectEntity(
                    folderId = folderId, name = "Cálculo I", colorArgb = 0xFF00BFA6.toInt(), professor = "Prof. Demo",
                ),
            )
            subjectRepository.upsertSession(
                ClassSessionEntity(subjectId = subjectId, dayOfWeek = 1, startMinuteOfDay = 7 * 60, endMinuteOfDay = 9 * 60, room = "302"),
            )
            subjectRepository.upsertSession(
                ClassSessionEntity(subjectId = subjectId, dayOfWeek = 3, startMinuteOfDay = 10 * 60, endMinuteOfDay = 12 * 60, room = "Lab 5"),
            )
            alarmController.saveTaskAndScheduleReminders(
                TaskEntity(
                    folderId = folderId,
                    subjectId = subjectId,
                    title = "Entregar taller 1",
                    dueAtEpochMillis = System.currentTimeMillis() + 3 * 24 * 60 * 60 * 1000L,
                ),
            )
            alarmController.saveAndSchedule(
                AlarmEntity(folderId = folderId, label = "Clase de Cálculo", hour = 6, minute = 30, repeatDaysBitmask = AlarmEntity.dayBit(1)),
            )
        }
    }

    fun deleteDemoData() {
        viewModelScope.launch { folderRepository.deleteByExactName("Demo 2026-1") }
    }

    fun simulateUsageData() {
        viewModelScope.launch {
            val today = todayEpochDay()
            val now = System.currentTimeMillis()
            val fakePackages = listOf(
                "com.instagram.android" to Random.nextLong(20, 120),
                "com.zhiliaoapp.musically" to Random.nextLong(10, 90),
                "com.google.android.youtube" to Random.nextLong(5, 60),
            )
            usageRepository.saveDailySnapshot(fakePackages.map { (pkg, min) -> AppUsageDailyEntity(today, pkg, min, now) })
        }
    }

    fun forceBlockOverlay() {
        context.startService(BlockOverlayService.intent(context, "Reels (prueba)", BlockSurface.INSTAGRAM_REELS))
    }
}
