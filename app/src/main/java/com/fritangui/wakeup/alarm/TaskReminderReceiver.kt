package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_TASK_ID
import com.fritangui.wakeup.data.db.dao.SubjectDao
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Recordatorio de vencimiento de una tarea (los offsets configurados, p.ej. 1 semana y 1 día antes). */
@AndroidEntryPoint
class TaskReminderReceiver : BroadcastReceiver() {

    @Inject lateinit var taskRepository: TaskRepository
    @Inject lateinit var subjectDao: SubjectDao
    @Inject lateinit var notificationHelper: NotificationHelper

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(EXTRA_TASK_ID, -1L)
        if (taskId < 0) return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val task = taskRepository.getById(taskId) ?: return@launch
                if (task.isCompleted) return@launch
                val subjectName = task.subjectId?.let { subjectDao.observeById(it).first()?.name }
                notificationHelper.notifyTaskReminder(task, subjectName)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
