package com.fritangui.wakeup.alarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmRingingViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
    settingsDataStore: SettingsDataStore,
) : ViewModel() {

    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    val alarm: StateFlow<AlarmEntity?> = _alarm.asStateFlow()

    /** Configurable en Ajustes (ver #123); 5 min por defecto. */
    val snoozeMinutes: StateFlow<Int> = settingsDataStore.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    fun load(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = alarmRepository.getById(alarmId)
        }
    }
}
