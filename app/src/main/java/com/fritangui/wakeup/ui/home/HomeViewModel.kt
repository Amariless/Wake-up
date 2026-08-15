package com.fritangui.wakeup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.domain.WeeklyClassDay
import com.fritangui.wakeup.domain.computeWeeklyClassSchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    subjectRepository: SubjectRepository,
    taskRepository: TaskRepository,
) : ViewModel() {

    private val subjectsWithSessions = subjectRepository.observeWithSessionsForActiveFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // #140: calendario semanal lunes-domingo (salta días sin clase) en vez de una lista de las
    // próximas N ocurrencias concretas.
    val weeklyClassDays: StateFlow<List<WeeklyClassDay>> = subjectsWithSessions
        .map { computeWeeklyClassSchedule(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingTasks: StateFlow<List<TaskEntity>> = taskRepository.observeUpcoming(limit = 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Color/nombre de materia por id, para decorar cada tarea igual que ya hacen los widgets. */
    val subjectColorsById: StateFlow<Map<Long, Int>> = subjectsWithSessions
        .map { list -> list.associate { it.subject.id to it.subject.colorArgb } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val subjectNamesById: StateFlow<Map<Long, String>> = subjectsWithSessions
        .map { list -> list.associate { it.subject.id to it.subject.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
