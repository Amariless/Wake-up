package com.fritangui.wakeup.ui.onboarding

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
class XiaomiOnboardingViewModel @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val autoStartConfirmed: StateFlow<Boolean> = settingsDataStore.isXiaomiAutoStartConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val backgroundPopupConfirmed: StateFlow<Boolean> = settingsDataStore.isXiaomiBackgroundPopupConfirmed
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    fun setAutoStartConfirmed(confirmed: Boolean) {
        viewModelScope.launch { settingsDataStore.setXiaomiAutoStartConfirmed(confirmed) }
    }

    fun setBackgroundPopupConfirmed(confirmed: Boolean) {
        viewModelScope.launch { settingsDataStore.setXiaomiBackgroundPopupConfirmed(confirmed) }
    }
}
