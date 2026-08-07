package com.fritangui.wakeup.alarm

import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.domain.computeReminderTriggers
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fachada única para todo lo que combina base de datos + AlarmManager. Las
 * ViewModels y casos de uso deben pasar por aquí en vez de tocar
 * [AlarmScheduler] directamente, así el estado en Room y el estado en el
 * sistema operativo nunca se desincronizan.
 */
@Singleton
class AlarmController @Inject constructor(
    private val alarmRepository: AlarmRepository,
    private val taskRepository: TaskRepository,
    private val folderRepository: FolderRepository,
    private val alarmScheduler: AlarmScheduler,
) {
    suspend fun saveAndSchedule(alarm: AlarmEntity): Long {
        val id = alarmRepository.upsert(alarm)
        alarmScheduler.scheduleAlarm(alarm.copy(id = id))
        return id
    }

    suspend fun deleteAndCancel(alarm: AlarmEntity) {
        alarmRepository.delete(alarm)
        alarmScheduler.cancelAlarm(alarm.id)
    }

    suspend fun setEnabledAndReschedule(alarmId: Long, enabled: Boolean) {
        alarmRepository.setEnabled(alarmId, enabled)
        val alarm = alarmRepository.getById(alarmId) ?: return
        if (enabled) alarmScheduler.scheduleAlarm(alarm) else alarmScheduler.cancelAlarm(alarmId)
    }

    /** Guarda la tarea y (re)programa sus recordatorios según su fecha de vencimiento y offsets. */
    suspend fun saveTaskAndScheduleReminders(task: TaskEntity): Long {
        val id = taskRepository.upsert(task)
        val saved = task.copy(id = id)
        alarmScheduler.cancelAllTaskReminders(id)
        computeReminderTriggers(saved).forEachIndexed { index, trigger ->
            alarmScheduler.scheduleTaskReminder(id, index, trigger)
        }
        return id
    }

    suspend fun deleteTaskAndCancelReminders(task: TaskEntity) {
        taskRepository.delete(task)
        alarmScheduler.cancelAllTaskReminders(task.id)
    }

    /**
     * Marca una carpeta como terminada: desactiva sus alarmas propias y cancela
     * tanto esas alarmas como los recordatorios pendientes de sus tareas. No
     * borra nada, solo apaga las notificaciones/alarmas futuras.
     */
    suspend fun terminateFolder(folderId: Long) {
        val alarms = alarmRepository.getAllForFolder(folderId)
        alarms.forEach { alarmScheduler.cancelAlarm(it.id) }
        val tasks = taskRepository.getAllForFolder(folderId)
        tasks.forEach { alarmScheduler.cancelAllTaskReminders(it.id) }
        folderRepository.markTerminated(folderId)
    }

    suspend fun reactivateFolder(folderId: Long) {
        folderRepository.reactivate(folderId)
        val alarms = alarmRepository.getAllForFolder(folderId).filter { it.isEnabled }
        alarms.forEach { alarmScheduler.scheduleAlarm(it) }
        val tasks = taskRepository.getAllForFolder(folderId).filter { !it.isCompleted }
        tasks.forEach { task ->
            computeReminderTriggers(task).forEachIndexed { index, trigger ->
                alarmScheduler.scheduleTaskReminder(task.id, index, trigger)
            }
        }
    }

    /** Reprograma absolutamente todo lo activo. Se usa tras un reinicio del teléfono. */
    suspend fun rescheduleEverything() {
        alarmRepository.getAllActiveEnabled().forEach { alarmScheduler.scheduleAlarm(it) }
        folderRepository.observeActive().first().forEach { folder ->
            taskRepository.getAllForFolder(folder.id).filter { !it.isCompleted }.forEach { task ->
                computeReminderTriggers(task).forEachIndexed { index, trigger ->
                    alarmScheduler.scheduleTaskReminder(task.id, index, trigger)
                }
            }
        }
    }
}
