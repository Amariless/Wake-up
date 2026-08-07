package com.fritangui.wakeup.alarm.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmRingingViewModel @Inject constructor(
    private val alarmRepository: AlarmRepository,
) : ViewModel() {

    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    val alarm: StateFlow<AlarmEntity?> = _alarm.asStateFlow()

    fun load(alarmId: Long) {
        viewModelScope.launch {
            _alarm.value = alarmRepository.getById(alarmId)
        }
    }
}
