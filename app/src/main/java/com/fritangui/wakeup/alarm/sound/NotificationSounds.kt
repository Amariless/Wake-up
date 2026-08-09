package com.fritangui.wakeup.alarm.sound

import android.content.Context
import com.fritangui.wakeup.R

/** Un sonido de notificación incluido con la app: cortos y suaves, a diferencia del banco de alarma. */
data class BundledNotificationSound(val id: String, val label: String, val rawResId: Int)

/**
 * Banco de sonidos para los "recordatorios" (notificación normal, sin pantalla completa ni reto
 * de apagado, ver [com.fritangui.wakeup.data.db.entity.AlarmEntity.kind]): son cortos y discretos
 * a propósito, porque a diferencia de una alarma no necesitan despertar a nadie.
 */
object NotificationSounds {
    val BUNDLED = listOf(
        BundledNotificationSound("soft_ping", "Ping suave (predeterminado)", R.raw.notif_soft_ping),
        BundledNotificationSound("double_pop", "Doble golpecito", R.raw.notif_double_pop),
        BundledNotificationSound("gentle_ding", "Ding", R.raw.notif_gentle_ding),
        BundledNotificationSound("subtle_tick", "Tick discreto", R.raw.notif_subtle_tick),
    )

    fun uriFor(context: Context, rawResId: Int): String = "android.resource://${context.packageName}/$rawResId"

    fun defaultSoundUriFor(context: Context): String = uriFor(context, BUNDLED.first().rawResId)

    fun labelFor(context: Context, soundUri: String?): String {
        if (soundUri == null) return BUNDLED.first().label
        val bundled = BUNDLED.firstOrNull { uriFor(context, it.rawResId) == soundUri }
        return bundled?.label ?: "Sonido del sistema"
    }
}
