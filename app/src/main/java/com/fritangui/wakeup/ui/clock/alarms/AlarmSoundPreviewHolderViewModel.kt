package com.fritangui.wakeup.ui.clock.alarms

import androidx.lifecycle.ViewModel
import com.fritangui.wakeup.alarm.sound.AlarmSoundPreviewPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * Envoltorio mínimo para poder obtener el [AlarmSoundPreviewPlayer] (singleton
 * de Hilt) desde un composable simple sin tener que crear un ViewModel propio
 * en cada pantalla que necesita previsualizar un sonido.
 */
@HiltViewModel
class AlarmSoundPreviewHolderViewModel @Inject constructor(
    val previewPlayer: AlarmSoundPreviewPlayer,
) : ViewModel() {
    override fun onCleared() {
        previewPlayer.stop()
        super.onCleared()
    }
}
