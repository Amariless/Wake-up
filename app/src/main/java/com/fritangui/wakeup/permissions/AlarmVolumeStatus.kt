package com.fritangui.wakeup.permissions

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService

/** Chequeo del volumen del stream de ALARMA (no el de notificaciones/media): ver #2 y #9. */
object AlarmVolumeStatus {

    /** true si el volumen de alarma está por debajo de [thresholdFraction] de su máximo (50% por defecto). */
    fun isLow(context: Context, thresholdFraction: Float = 0.5f): Boolean {
        val audioManager = context.getSystemService<AudioManager>() ?: return false
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return false
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        return current.toFloat() / max < thresholdFraction
    }
}
