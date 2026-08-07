package com.fritangui.wakeup.ui.clock.timer

import android.content.Context
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.alarm.TimerForegroundService
import com.fritangui.wakeup.data.datastore.SettingsDataStore
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerChallengePref(val type: DismissChallengeType, val difficulty: Int)

@HiltViewModel
class TimerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsDataStore: SettingsDataStore,
) : ViewModel() {

    val timerState = TimerForegroundService.state

    val challengePref: StateFlow<TimerChallengePref> = combine(
        settingsDataStore.timerChallenge,
        settingsDataStore.timerChallengeDifficulty,
    ) { type, difficulty -> TimerChallengePref(type, difficulty) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerChallengePref(DismissChallengeType.NONE, 1))

    fun setChallengePref(type: DismissChallengeType, difficulty: Int) {
        viewModelScope.launch { settingsDataStore.setTimerChallenge(type, difficulty) }
    }

    fun start(durationMillis: Long, type: DismissChallengeType, difficulty: Int) {
        ContextCompat.startForegroundService(context, TimerForegroundService.startIntent(context, durationMillis, type, difficulty))
    }

    fun pause() = sendAction(TimerForegroundService.ACTION_PAUSE)
    fun resume() = sendAction(TimerForegroundService.ACTION_RESUME)
    fun cancel() = sendAction(TimerForegroundService.ACTION_CANCEL)

    private fun sendAction(action: String) {
        // El servicio ya está en primer plano en este punto (la UI que llama a esto está visible),
        // así que un startService normal basta; no hace falta re-promoverlo.
        context.startService(TimerForegroundService.actionIntent(context, action))
    }
}
