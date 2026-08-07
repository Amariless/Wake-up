package com.fritangui.wakeup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.domain.computeNextClassOccurrences
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

    val nextClasses: StateFlow<List<UpcomingClassOccurrence>> = subjectRepository
        .observeWithSessionsForActiveFolders()
        .map { computeNextClassOccurrences(it, limit = 6) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val upcomingTasks: StateFlow<List<TaskEntity>> = taskRepository.observeUpcoming(limit = 6)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}
