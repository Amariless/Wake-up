package com.fritangui.wakeup.ui.clock.stopwatch

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StopwatchState(
    val elapsedMillis: Long = 0L,
    val isRunning: Boolean = false,
    val laps: List<Long> = emptyList(),
)

/**
 * Cronómetro simple: no necesita foreground service (a diferencia de alarmas/
 * temporizador, no tiene un momento en el que "suena" y deba sobrevivir a que
 * el usuario lo pierda de vista); sobrevive a rotaciones de pantalla porque
 * vive en el ViewModel, pero se reinicia si el proceso de la app muere.
 */
@HiltViewModel
class StopwatchViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(StopwatchState())
    val state: StateFlow<StopwatchState> = _state.asStateFlow()

    private var tickJob: Job? = null
    private var startedAtElapsedMillis = 0L
    private var accumulatedMillis = 0L

    fun start() {
        if (_state.value.isRunning) return
        // elapsedRealtime(), no currentTimeMillis(): este último es el reloj de pared (UTC) y salta
        // de golpe si el usuario ajusta la hora o el teléfono resincroniza por NTP mientras el
        // cronómetro corre; elapsedRealtime() no depende de la hora de pared.
        startedAtElapsedMillis = SystemClock.elapsedRealtime()
        _state.value = _state.value.copy(isRunning = true)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                if (_state.subscriptionCount.value > 0) {
                    val now = SystemClock.elapsedRealtime()
                    _state.value = _state.value.copy(elapsedMillis = accumulatedMillis + (now - startedAtElapsedMillis))
                    delay(31)
                } else {
                    // Nadie está observando el estado (pantalla en segundo plano): no hace falta
                    // actualizar decenas de veces por segundo, solo despertar de vez en cuando por si
                    // vuelve a haber un colector. El valor sigue siendo correcto al volver porque se
                    // recalcula siempre desde startedAtElapsedMillis, no por acumulación de ticks.
                    delay(1_000)
                }
            }
        }
    }

    fun pause() {
        if (!_state.value.isRunning) return
        accumulatedMillis = _state.value.elapsedMillis
        tickJob?.cancel()
        _state.value = _state.value.copy(isRunning = false)
    }

    fun lap() {
        if (!_state.value.isRunning) return
        _state.value = _state.value.copy(laps = _state.value.laps + _state.value.elapsedMillis)
    }

    fun reset() {
        tickJob?.cancel()
        accumulatedMillis = 0L
        _state.value = StopwatchState()
    }
}
