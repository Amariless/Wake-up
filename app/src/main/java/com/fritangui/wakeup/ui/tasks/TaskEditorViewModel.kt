package com.fritangui.wakeup.ui.tasks

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.dao.SubjectDao
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val taskRepository: TaskRepository,
    private val alarmController: AlarmController,
    private val widgetRefresher: WidgetRefresher,
    private val sessionState: TaskCreationSessionState,
    subjectDao: SubjectDao,
) : ViewModel() {

    val folderId: Long = checkNotNull(savedStateHandle["folderId"])
    private val taskId: Long = checkNotNull(savedStateHandle["taskId"])
    val isNew: Boolean = taskId == 0L

    /** Solo aplica a tareas nuevas: la materia usada la última vez en ESTA misma carpeta. */
    val initialSubjectIdForNew: Long? = if (isNew) sessionState.lastSubjectFor(folderId) else null
    val skipNoSubjectConfirmation: Boolean get() = sessionState.skipNoSubjectConfirmation

    private val _task = MutableStateFlow<TaskEntity?>(null)
    val task: StateFlow<TaskEntity?> = _task.asStateFlow()

    val subjectsInFolder: StateFlow<List<SubjectEntity>> = subjectDao.observeByFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        if (!isNew) {
            viewModelScope.launch { _task.value = taskRepository.getById(taskId) }
        }
    }

    fun dontAskAgainThisSession() = sessionState.dontAskAgainThisSession()

    fun save(
        title: String,
        description: String,
        notes: String,
        isNoteImportant: Boolean,
        subjectId: Long?,
        dueAtEpochMillis: Long?,
        reminderOffsetsMinutes: List<Long>,
        onSaved: () -> Unit,
    ) {
        if (title.isBlank()) return
        sessionState.remember(folderId, subjectId)
        viewModelScope.launch {
            val entity = TaskEntity(
                id = taskId,
                folderId = folderId,
                subjectId = subjectId,
                title = title.trim(),
                description = description.trim(),
                notes = notes.trim(),
                isNoteImportant = isNoteImportant,
                dueAtEpochMillis = dueAtEpochMillis,
                reminderOffsetsMinutes = if (dueAtEpochMillis == null) emptyList() else reminderOffsetsMinutes,
                isCompleted = _task.value?.isCompleted ?: false,
            )
            alarmController.saveTaskAndScheduleReminders(entity)
            widgetRefresher.refreshAll()
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = _task.value ?: return
        viewModelScope.launch {
            alarmController.deleteTaskAndCancelReminders(current)
            widgetRefresher.refreshAll()
            onDeleted()
        }
    }
}
