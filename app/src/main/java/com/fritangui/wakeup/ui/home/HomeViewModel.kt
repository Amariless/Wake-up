package com.fritangui.wakeup.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.data.repository.SubjectRepository
import com.fritangui.wakeup.data.repository.TaskRepository
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.domain.WeeklyClassDay
import com.fritangui.wakeup.domain.computeNextClassOccurrences
import com.fritangui.wakeup.domain.computeWeeklyClassSchedule
import com.fritangui.wakeup.domain.todayEpochDay
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
    usageRepository: UsageRepository,
) : ViewModel() {

    /** Minutos de pantalla acumulados hoy (todas las apps), para la tarjeta de Bienestar de Inicio
     *  — misma fuente que usa la pestaña Bienestar, solo que sumada en un único total. */
    val todayScreenTimeMinutes: StateFlow<Long> = usageRepository.observeForDay(todayEpochDay())
        .map { rows -> rows.sumOf { it.minutesUsed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    private val subjectsWithSessions = subjectRepository.observeWithSessionsForActiveFolders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // #140: calendario semanal lunes-domingo (salta días sin clase) en vez de una lista de las
    // próximas N ocurrencias concretas.
    val weeklyClassDays: StateFlow<List<WeeklyClassDay>> = subjectsWithSessions
        .map { computeWeeklyClassSchedule(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** La clase que está pasando ahora mismo, o si no hay ninguna, la próxima — para la tarjeta
     *  destacada de Inicio (rediseño). null si no hay ninguna materia con horario.
     *
     *  Se recalcula cuando cambian las materias/horarios (BD), no en vivo con el reloj — igual que
     *  el resto de Inicio (ver [weeklyClassDays]). La cuenta regresiva SÍ tiene que verse viva
     *  mientras se mira la pantalla, pero eso lo resuelve HomeScreen con su propio tick local: si
     *  se hiciera acá combinando con un Flow que emite cada 30s, StateFlow no volvería a notificar
     *  a los que escuchan cuando el resultado da exactamente la misma ocurrencia (que es el caso
     *  normal entre un tick y el siguiente) — conflation por igualdad, no por tiempo transcurrido. */
    val nextClassOccurrence: StateFlow<UpcomingClassOccurrence?> = subjectsWithSessions
        .map { computeNextClassOccurrences(it, limit = 1).firstOrNull() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // Antes 6: con "Próximas tareas" ahora separada por vencimiento (#161), 6 se quedaba corto
    // para llenar más de un grupo — mismo límite que ya usaba el widget de tareas.
    val upcomingTasks: StateFlow<List<TaskEntity>> = taskRepository.observeUpcoming(limit = 12)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Color/nombre de materia por id, para decorar cada tarea igual que ya hacen los widgets. */
    val subjectColorsById: StateFlow<Map<Long, Int>> = subjectsWithSessions
        .map { list -> list.associate { it.subject.id to it.subject.colorArgb } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val subjectNamesById: StateFlow<Map<Long, String>> = subjectsWithSessions
        .map { list -> list.associate { it.subject.id to it.subject.name } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
}
