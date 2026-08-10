package com.fritangui.wakeup.alarm.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reproduce un sonido una sola vez, para el botón de "previsualizar" del editor y la lista de
 * alarmas. A propósito usa `USAGE_MEDIA` (el volumen normal/multimedia del teléfono) y NO
 * `USAGE_ALARM`: la previsualización es solo para escuchar cómo suena, no la alarma real sonando
 * — si usara el stream de alarma, se oiría al volumen de alarma (que en muchos teléfonos está al
 * máximo o en un nivel totalmente distinto al que el usuario tiene puesto para música/notis) y
 * quedaba desproporcionadamente fuerte o silenciosa según el volumen de alarma configurado en el
 * sistema, no el que el usuario esperaría al tocar "previsualizar". El sonido real de la alarma
 * (cuando de verdad suena, ver [com.fritangui.wakeup.alarm.RingingForegroundService]) sigue usando
 * `USAGE_ALARM` sin cambios.
 */
@Singleton
class AlarmSoundPreviewPlayer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private var player: MediaPlayer? = null

    private val _playingUri = MutableStateFlow<String?>(null)
    val playingUri: StateFlow<String?> = _playingUri.asStateFlow()

    fun toggle(uriString: String) {
        if (_playingUri.value == uriString) stop() else play(uriString)
    }

    fun play(uriString: String) {
        stop()
        runCatching {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(context, Uri.parse(uriString))
                isLooping = true
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            _playingUri.value = uriString
        }
    }

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        _playingUri.value = null
    }
}
