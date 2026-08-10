package com.fritangui.wakeup.ui.folders

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.dao.SubjectWithSessions
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.FolderEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FolderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    folderRepository: FolderRepository,
    subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    alarmRepository: AlarmRepository,
    private val alarmController: AlarmController,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val folderId: Long = checkNotNull(savedStateHandle["folderId"])

    val folder: StateFlow<FolderEntity?> = folderRepository.observeById(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPinned: StateFlow<Boolean> = settingsDataStore.pinnedFolderId
        .map { it == folderId }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun togglePinned() {
        viewModelScope.launch {
            settingsDataStore.setPinnedFolderId(if (isPinned.value) null else folderId)
        }
    }

    val subjects: StateFlow<List<SubjectWithSessions>> = subjectRepository.observeWithSessionsByFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val tasks: StateFlow<List<TaskEntity>> = taskRepository.observeByFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Nombre de materia por id, para mostrar debajo de cada tarea que tenga una asociada. */
    val subjectNamesById: StateFlow<Map<Long, String>> = subjects
        .map { list -> list.associate { it.subject.id to it.subject.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    /** Color de materia por id, para la barra lateral de cada tarea en la lista. */
    val subjectColorsById: StateFlow<Map<Long, Int>> = subjects
        .map { list -> list.associate { it.subject.id to it.subject.colorArgb } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.observeByFolder(folderId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch { taskRepository.setCompleted(taskId, completed) }
    }

    fun setAlarmEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch { alarmController.setEnabledAndReschedule(alarmId, enabled) }
    }
}
