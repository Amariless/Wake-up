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

/** Reproduce un sonido de alarma una sola vez, para el botón de "previsualizar" del editor y la lista de alarmas. */
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
                        .setUsage(AudioAttributes.USAGE_ALARM)
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
