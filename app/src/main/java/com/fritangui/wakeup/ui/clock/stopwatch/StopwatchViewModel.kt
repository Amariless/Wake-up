package com.fritangui.wakeup.ui.clock.stopwatch

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
        startedAtElapsedMillis = System.currentTimeMillis()
        _state.value = _state.value.copy(isRunning = true)
        tickJob?.cancel()
        tickJob = viewModelScope.launch {
            while (_state.value.isRunning) {
                val now = System.currentTimeMillis()
                _state.value = _state.value.copy(elapsedMillis = accumulatedMillis + (now - startedAtElapsedMillis))
                delay(31)
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
