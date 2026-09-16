package com.fritangui.wakeup.permissions

import android.content.Context
import android.media.AudioManager
import androidx.core.content.getSystemService
import kotlin.math.roundToInt

/** Chequeo del volumen del stream de ALARMA (no el de notificaciones/media): ver #2 y #9.
 *
 *  Android trata STREAM_ALARM como un volumen totalmente aparte del de música/sistema — subir el
 *  volumen "general" con los botones físicos NO toca este stream. En bastantes Samsung (#161: el
 *  usuario reportó esto en un S23 Ultra) el control de Alarma ni siquiera aparece a simple vista en
 *  el panel de volumen rápido (hay que expandirlo), así que aunque la detección de acá sea
 *  perfectamente correcta, se siente como un falso positivo si no se sabe que es un volumen
 *  independiente. Por eso [raiseToComfortable] evita mandar a la persona a buscar ese control a
 *  ciegas: lo sube directo desde la app. */
object AlarmVolumeStatus {

    /** true si el volumen de alarma está por debajo de [thresholdFraction] de su máximo (50% por defecto). */
    fun isLow(context: Context, thresholdFraction: Float = 0.5f): Boolean {
        val audioManager = context.getSystemService<AudioManager>() ?: return false
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return false
        val current = audioManager.getStreamVolume(AudioManager.STREAM_ALARM)
        return current.toFloat() / max < thresholdFraction
    }

    /** Sube el volumen de ALARMA directo al 80% de su máximo (fuerte, pero no el 100% de una) y
     *  muestra el indicador nativo del sistema (FLAG_SHOW_UI) para que quede claro qué stream
     *  cambió. Evita depender de que la persona encuentre el control correcto a mano. */
    fun raiseToComfortable(context: Context, targetFraction: Float = 0.8f) {
        val audioManager = context.getSystemService<AudioManager>() ?: return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM)
        if (max <= 0) return
        val target = (max * targetFraction).roundToInt().coerceIn(1, max)
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, target, AudioManager.FLAG_SHOW_UI)
    }
}
