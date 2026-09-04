package com.fritangui.wakeup.alarm

import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.data.repository.SubjectRepository
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
    private val subjectRepository: SubjectRepository,
    private val settingsDataStore: SettingsDataStore,
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

    /** Cancela el aviso de "próxima clase" (#144) de una sesión puntual, p.ej. antes de borrarla. */
    fun cancelClassReminder(sessionId: Long) = alarmScheduler.cancelClassReminder(sessionId)

    /**
     * Marca una carpeta como terminada: desactiva sus alarmas propias y cancela
     * esas alarmas, los recordatorios pendientes de sus tareas y los avisos de
     * "próxima clase" (#144) de sus materias. No borra nada, solo apaga las
     * notificaciones/alarmas futuras.
     */
    suspend fun terminateFolder(folderId: Long) {
        val alarms = alarmRepository.getAllForFolder(folderId)
        alarms.forEach { alarmScheduler.cancelAlarm(it.id) }
        val tasks = taskRepository.getAllForFolder(folderId)
        tasks.forEach { alarmScheduler.cancelAllTaskReminders(it.id) }
        val sessions = subjectRepository.observeWithSessionsByFolder(folderId).first().flatMap { it.sessions }
        sessions.forEach { alarmScheduler.cancelClassReminder(it.id) }
        folderRepository.markTerminated(folderId)
    }

    /**
     * Borra una carpeta y, antes, cancela en AlarmManager todo lo que colgaba de ella
     * (alarmas propias, recordatorios de tareas y avisos de "próxima clase"). El borrado
     * en Room de tareas/alarmas/materias/sesiones ocurre solo por el CASCADE de las
     * foreign keys, que nunca toca AlarmManager por su cuenta — sin este paso, esos
     * PendingIntent quedarían armados apuntando a filas que ya no existen.
     */
    suspend fun deleteFolderAndCancelAll(folder: FolderEntity) {
        val alarms = alarmRepository.getAllForFolder(folder.id)
        alarms.forEach { alarmScheduler.cancelAlarm(it.id) }
        val tasks = taskRepository.getAllForFolder(folder.id)
        tasks.forEach { alarmScheduler.cancelAllTaskReminders(it.id) }
        val sessions = subjectRepository.observeWithSessionsByFolder(folder.id).first().flatMap { it.sessions }
        sessions.forEach { alarmScheduler.cancelClassReminder(it.id) }
        folderRepository.delete(folder)
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
        rescheduleClassReminders()
    }

    /**
     * (Re)programa el aviso de "próxima clase en X min" (#144) para todas las sesiones de carpetas
     * activas, según el valor actual del ajuste. Se llama al arrancar la app, tras reiniciar el
     * teléfono, y cada vez que el usuario cambia los minutos en Ajustes — así una sesión ya armada
     * con el valor viejo se sobrescribe con el nuevo en vez de sonar tarde/temprano de más.
     */
    suspend fun rescheduleClassReminders() {
        val minutesBefore = settingsDataStore.nextClassNotificationMinutesBefore.first()
        val sessions = subjectRepository.observeWithSessionsForActiveFolders().first().flatMap { it.sessions }
        if (minutesBefore <= 0) {
            // Apagado (o recién apagado desde Ajustes): cancela lo que ya estuviera armado en vez
            // de dejarlo sonar una vez más de más antes de autocorregirse.
            sessions.forEach { alarmScheduler.cancelClassReminder(it.id) }
            return
        }
        sessions.forEach { session -> alarmScheduler.scheduleClassReminder(session, minutesBefore) }
    }
}
