package com.fritangui.wakeup.ui.clock.alarms

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.AlarmController
import com.fritangui.wakeup.data.db.entity.AlarmEntity
import com.fritangui.wakeup.data.db.entity.AlarmKind
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.domain.AlarmTiming
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
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

    // Mensaje breve ("Alarma guardada: suena en 2h 15m") que la pantalla muestra justo después de
    // guardar, antes de volver — así queda claro de una que la próxima ocurrencia quedó bien
    // calculada sin tener que ir a la lista a fijarse.
    private val _savedFeedback = MutableStateFlow<String?>(null)
    val savedFeedback: StateFlow<String?> = _savedFeedback.asStateFlow()

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
        kind: com.fritangui.wakeup.data.db.entity.AlarmKind,
        challenge: com.fritangui.wakeup.data.db.entity.DismissChallengeType,
        difficulty: Int,
        vibrate: Boolean,
        soundUri: String?,
        preAlarmMinutesBefore: Int,
        deleteAfterRing: Boolean,
        onSaved: () -> Unit,
    ) {
        viewModelScope.launch {
            val existing = _alarm.value
            val entity = AlarmEntity(
                id = alarmId,
                folderId = folderId,
                // "Despertar" es solo el placeholder que se ve en el campo (nunca hay que borrarlo
                // para escribir un nombre propio); si de verdad se deja vacío, se guarda ese mismo
                // texto en vez de una cadena vacía.
                label = label.trim().ifBlank { "Despertar" },
                hour = hour,
                minute = minute,
                repeatDaysBitmask = repeatDaysBitmask,
                isEnabled = true,
                kind = kind,
                dismissChallenge = challenge,
                challengeDifficulty = difficulty,
                vibrate = vibrate,
                soundUri = soundUri,
                // Al recordatorio no le hace falta aviso previo de 1h: es él mismo el aviso.
                preAlarmNotificationMinutesBefore = if (kind == com.fritangui.wakeup.data.db.entity.AlarmKind.REMINDER) 0 else preAlarmMinutesBefore,
                deleteAfterRing = deleteAfterRing,
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: System.currentTimeMillis(),
                lastTriggeredAtEpochMillis = existing?.lastTriggeredAtEpochMillis,
            )
            alarmController.saveAndSchedule(entity)

            val kindLabel = if (entity.kind == AlarmKind.REMINDER) "Recordatorio guardado" else "Alarma guardada"
            val trigger = AlarmTiming.nextTrigger(entity)
            _savedFeedback.value = if (trigger != null) {
                "$kindLabel: suena en ${AlarmTiming.formatRemaining(trigger - Clock.System.now())}"
            } else {
                "$kindLabel, pero no está programada (revisa los días marcados)"
            }
            // Pequeña pausa para que el mensaje de arriba alcance a leerse antes de salir de la
            // pantalla; onSaved() normalmente navega hacia atrás.
            delay(1100)
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
