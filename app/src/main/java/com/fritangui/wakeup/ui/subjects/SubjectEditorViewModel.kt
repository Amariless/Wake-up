package com.fritangui.wakeup.ui.subjects

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.data.db.entity.SubjectEntity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.ui.tasks.TaskCreationSessionState
import com.fritangui.wakeup.widget.WidgetRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SubjectEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val subjectRepository: SubjectRepository,
    private val taskRepository: TaskRepository,
    private val widgetRefresher: WidgetRefresher,
    private val taskCreationSessionState: TaskCreationSessionState,
    private val alarmController: AlarmController,
) : ViewModel() {

    val folderId: Long = checkNotNull(savedStateHandle["folderId"])
    private val initialSubjectId: Long = checkNotNull(savedStateHandle["subjectId"])

    private val _currentSubjectId = MutableStateFlow(initialSubjectId.takeIf { it != 0L })
    val currentSubjectId: StateFlow<Long?> = _currentSubjectId.asStateFlow()

    val sessions: StateFlow<List<ClassSessionEntity>> = _currentSubjectId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else subjectRepository.observeSessions(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val subject: StateFlow<SubjectEntity?> = _currentSubjectId
        .flatMapLatest { id -> if (id == null) flowOf(null) else subjectRepository.observeById(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Tareas asociadas a esta materia, para mostrarlas debajo del horario (ver #71). */
    val tasks: StateFlow<List<TaskEntity>> = _currentSubjectId
        .flatMapLatest { id -> if (id == null) flowOf(emptyList()) else taskRepository.observeBySubject(id) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTaskCompleted(taskId: Long, completed: Boolean) {
        viewModelScope.launch { taskRepository.setCompleted(taskId, completed) }
    }

    fun completeTaskWithGrade(task: TaskEntity, gradeValue: Double?, gradeWeightPercent: Double?) {
        viewModelScope.launch { taskRepository.setCompletedWithGrade(task.id, true, gradeValue, gradeWeightPercent) }
    }

    fun deleteTask(task: TaskEntity) {
        viewModelScope.launch { alarmController.deleteTaskAndCancelReminders(task) }
    }

    /** Llamar justo antes de navegar a crear una tarea nueva desde aquí: la deja preseleccionada con esta materia. */
    fun presetNewTaskToThisSubject() {
        val id = _currentSubjectId.value ?: return
        taskCreationSessionState.remember(folderId, id)
    }

    /**
     * Guarda nombre/profesor/color. Si es una materia nueva, la crea (upsert) y
     * habilita la sección de horarios. Si ya existe, usa `update` en vez de
     * `upsert` a propósito: `upsert` usa `Insert(REPLACE)`, que ante un choque
     * de clave primaria borra la fila vieja e inserta una nueva — y como
     * [com.fritangui.wakeup.data.db.entity.ClassSessionEntity] tiene
     * `onDelete = CASCADE` hacia la materia, eso borraba silenciosamente todos
     * sus horarios cada vez que solo querías cambiar el nombre o el color.
     */
    fun saveBasicInfo(name: String, professor: String, colorArgb: Int, iconKey: String?, onSaved: (Long) -> Unit) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val id = _currentSubjectId.value
            if (id != null) {
                subjectRepository.update(
                    SubjectEntity(id = id, folderId = folderId, name = name.trim(), colorArgb = colorArgb, professor = professor.trim(), iconKey = iconKey),
                )
                onSaved(id)
            } else {
                val entity = SubjectEntity(folderId = folderId, name = name.trim(), colorArgb = colorArgb, professor = professor.trim(), iconKey = iconKey)
                val savedId = subjectRepository.upsert(entity)
                _currentSubjectId.value = savedId
                onSaved(savedId)
            }
        }
    }

    fun addOrUpdateSession(session: ClassSessionEntity) {
        val subjectId = _currentSubjectId.value ?: return
        viewModelScope.launch {
            subjectRepository.upsertSession(session.copy(subjectId = subjectId))
            widgetRefresher.refreshAll()
        }
    }

    fun deleteSession(session: ClassSessionEntity) {
        viewModelScope.launch {
            subjectRepository.deleteSession(session)
            widgetRefresher.refreshAll()
        }
    }

    fun deleteSubject(subject: SubjectEntity, onDeleted: () -> Unit) {
        viewModelScope.launch {
            subjectRepository.delete(subject)
            onDeleted()
        }
    }
}
