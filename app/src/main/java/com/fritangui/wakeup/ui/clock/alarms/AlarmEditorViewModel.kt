package com.fritangui.wakeup.ui.clock.alarms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.repository.AlarmRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AlarmEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val alarmRepository: AlarmRepository,
    private val alarmController: AlarmController,
) : ViewModel() {

    private val routeFolderId: Long = checkNotNull(savedStateHandle["folderId"])
    private val alarmId: Long = checkNotNull(savedStateHandle["alarmId"])
    val isNew: Boolean = alarmId == 0L

    /** null = alarma del reloj general. */
    val folderId: Long? = routeFolderId.takeIf { it != 0L }

    private val _alarm = MutableStateFlow<AlarmEntity?>(null)
    val alarm: StateFlow<AlarmEntity?> = _alarm.asStateFlow()

    init {
        if (!isNew) {
            viewModelScope.launch { _alarm.value = alarmRepository.getById(alarmId) }
        }
    }

    fun save(
        label: String,
        hour: Int,
        minute: Int,
        repeatDaysBitmask: Int,
        challenge: com.fritangui.wakeup.data.db.entity.DismissChallengeType,
        difficulty: Int,
        vibrate: Boolean,
        preAlarmMinutesBefore: Int,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val entity = AlarmEntity(
                id = alarmId,
                folderId = folderId,
                label = label.trim(),
                hour = hour,
                minute = minute,
                repeatDaysBitmask = repeatDaysBitmask,
                isEnabled = true,
                dismissChallenge = challenge,
                challengeDifficulty = difficulty,
                vibrate = vibrate,
                preAlarmNotificationMinutesBefore = preAlarmMinutesBefore,
            )
            alarmController.saveAndSchedule(entity)
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = _alarm.value ?: return
        viewModelScope.launch {
            alarmController.deleteAndCancel(current)
            onDeleted()
        }
    }
}
