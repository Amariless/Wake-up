package com.fritangui.wakeup.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val alarmController: AlarmController,
) : ViewModel() {

    val dynamicColorEnabled: StateFlow<Boolean> = settingsDataStore.isDynamicColorEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setDynamicColorEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setDynamicColorEnabled(enabled) }
    }

    val use24HourFormat: StateFlow<Boolean> = settingsDataStore.use24HourFormat
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setUse24HourFormat(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setUse24HourFormat(enabled) }
    }

    val snoozeMinutes: StateFlow<Int> = settingsDataStore.snoozeMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    fun setSnoozeMinutes(minutes: Int) {
        viewModelScope.launch { settingsDataStore.setSnoozeMinutes(minutes) }
    }

    val blockGraceMinutes: StateFlow<Int> = settingsDataStore.blockGraceMinutes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 5)

    fun setBlockGraceMinutes(minutes: Int) {
        viewModelScope.launch { settingsDataStore.setBlockGraceMinutes(minutes) }
    }

    /** 0 = aviso apagado (por defecto); ver #144. */
    val nextClassNotificationMinutes: StateFlow<Int> = settingsDataStore.nextClassNotificationMinutesBefore
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    fun setNextClassNotificationEnabled(enabled: Boolean) {
        viewModelScope.launch {
            // Al activarlo por primera vez sin un valor previo, 15 min es un aviso razonable (ni
            // tan pegado como para no dar tiempo a llegar, ni tan temprano como para sentirse
            // desconectado de la clase). Al apagarlo simplemente se guarda 0.
            val minutes = if (enabled) (if (nextClassNotificationMinutes.value <= 0) 15 else nextClassNotificationMinutes.value) else 0
            settingsDataStore.setNextClassNotificationMinutesBefore(minutes)
            alarmController.rescheduleClassReminders()
        }
    }

    fun setNextClassNotificationMinutes(minutes: Int) {
        viewModelScope.launch {
            settingsDataStore.setNextClassNotificationMinutesBefore(minutes)
            alarmController.rescheduleClassReminders()
        }
    }

    /** Ver #9: aviso si el volumen de alarma está bajo y hay una alarma por sonar pronto. */
    val lowAlarmVolumeWarningEnabled: StateFlow<Boolean> = settingsDataStore.lowAlarmVolumeWarningEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setLowAlarmVolumeWarningEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsDataStore.setLowAlarmVolumeWarningEnabled(enabled) }
    }

    val lowAlarmVolumeWarningHoursAhead: StateFlow<Int> = settingsDataStore.lowAlarmVolumeWarningHoursAhead
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 12)

    fun setLowAlarmVolumeWarningHoursAhead(hours: Int) {
        viewModelScope.launch { settingsDataStore.setLowAlarmVolumeWarningHoursAhead(hours) }
    }
}
