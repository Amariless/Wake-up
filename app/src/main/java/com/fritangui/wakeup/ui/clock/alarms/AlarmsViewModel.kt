package com.fritangui.wakeup.ui.clock.alarms

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Alarmas del "reloj general" (fuera de cualquier carpeta/semestre). */
@HiltViewModel
class AlarmsViewModel @Inject constructor(
    alarmRepository: AlarmRepository,
    private val alarmController: AlarmController,
) : ViewModel() {

    val alarms: StateFlow<List<AlarmEntity>> = alarmRepository.observeGeneralAlarms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(alarmId: Long, enabled: Boolean) {
        viewModelScope.launch { alarmController.setEnabledAndReschedule(alarmId, enabled) }
    }
}
