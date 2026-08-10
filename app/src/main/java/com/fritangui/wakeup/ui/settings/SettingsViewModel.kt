package com.fritangui.wakeup.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
}
