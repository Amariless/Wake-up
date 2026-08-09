package com.fritangui.wakeup.alarm.sound

import android.content.Context
import com.fritangui.wakeup.R

/** Un sonido de alarma incluido con la app (sintetizado, sin restricciones de licencia). */
data class BundledAlarmSound(val id: String, val label: String, val rawResId: Int)

/**
 * Banco de sonidos propio de la app. El sonido por defecto del sistema en
 * muchos emuladores/teléfonos es muy suave o no existe; estos se generan
 * por código (sin depender de ninguna licencia externa) para tener siempre
 * algo audible y variado. El usuario también puede elegir cualquier tono de
 * alarma instalado en su teléfono (ver [AlarmSoundPicker]).
 */
object AlarmSounds {
    val BUNDLED = listOf(
        BundledAlarmSound("urgent_pulse", "Urgente (predeterminado)", R.raw.alarm_urgent_pulse),
        BundledAlarmSound("classic_beep", "Clásico", R.raw.alarm_classic_beep),
        BundledAlarmSound("gentle_chime", "Suave", R.raw.alarm_gentle_chime),
        BundledAlarmSound("digital_beep", "Digital", R.raw.alarm_digital_beep),
        BundledAlarmSound("rising_siren", "Sirena", R.raw.alarm_rising_siren),
        BundledAlarmSound("marimba_urgent", "Marimba", R.raw.alarm_marimba_urgent),
        BundledAlarmSound("classic_bell", "Campana", R.raw.alarm_classic_bell),
    )

    fun uriFor(context: Context, rawResId: Int): String = "android.resource://${context.packageName}/$rawResId"

    fun defaultSoundUriFor(context: Context): String = uriFor(context, BUNDLED.first().rawResId)

    /** Etiqueta legible para mostrar en la UI a partir de un `soundUri` guardado (o "Sonido del sistema" si es otro). */
    fun labelFor(context: Context, soundUri: String?): String {
        if (soundUri == null) return BUNDLED.first().label
        val bundled = BUNDLED.firstOrNull { uriFor(context, it.rawResId) == soundUri }
        return bundled?.label ?: "Sonido del sistema"
    }
}
