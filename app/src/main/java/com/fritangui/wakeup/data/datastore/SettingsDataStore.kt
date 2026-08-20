package com.fritangui.wakeup.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "wakeup_settings")

/**
 * Configuración global de la app que no encaja como fila de una tabla:
 * onboarding completado, tema, última carpeta seleccionada, etc.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val XIAOMI_ONBOARDING_DONE = booleanPreferencesKey("xiaomi_onboarding_done")
        val DYNAMIC_COLOR_ENABLED = booleanPreferencesKey("dynamic_color_enabled")
        val PINNED_FOLDER_ID = stringPreferencesKey("last_selected_folder_id")
        val DEFAULT_REMINDER_WEEK_BEFORE_MIN = intPreferencesKey("default_reminder_week_min")
        val DEFAULT_REMINDER_DAY_BEFORE_MIN = intPreferencesKey("default_reminder_day_min")
        val TIMER_CHALLENGE_TYPE = stringPreferencesKey("timer_challenge_type")
        val TIMER_CHALLENGE_DIFFICULTY = intPreferencesKey("timer_challenge_difficulty")
        val XIAOMI_AUTOSTART_CONFIRMED = booleanPreferencesKey("xiaomi_autostart_confirmed")
        val XIAOMI_BACKGROUND_POPUP_CONFIRMED = booleanPreferencesKey("xiaomi_background_popup_confirmed")
        val USE_24_HOUR_FORMAT = booleanPreferencesKey("use_24_hour_format")
        val SNOOZE_MINUTES = intPreferencesKey("snooze_minutes")
        val BLOCK_GRACE_MINUTES = intPreferencesKey("block_grace_minutes")
        val NEXT_CLASS_NOTIFICATION_MINUTES = intPreferencesKey("next_class_notification_minutes")
        val LAST_TIMER_DURATION_MILLIS = intPreferencesKey("last_timer_duration_seconds")
        val LOW_ALARM_VOLUME_WARNING_ENABLED = booleanPreferencesKey("low_alarm_volume_warning_enabled")
        val LOW_ALARM_VOLUME_WARNING_HOURS_AHEAD = intPreferencesKey("low_alarm_volume_warning_hours_ahead")
        val LAST_KNOWN_GRANTED_PERMISSIONS = stringSetPreferencesKey("last_known_granted_permissions")
        val LAST_NOTIFIED_LOW_VOLUME_TRIGGER_MILLIS = longPreferencesKey("last_notified_low_volume_trigger_millis")
    }

    val isXiaomiOnboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.XIAOMI_ONBOARDING_DONE] ?: false }

    suspend fun setXiaomiOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.XIAOMI_ONBOARDING_DONE] = done }
    }

    // Por defecto apagado: la paleta propia de Wake up (índigo + ámbar) es la identidad visual de
    // la app; el color dinámico de Material You suele verse más apagado/genérico según el fondo
    // de pantalla del usuario, justo lo contrario de lo que se busca aquí. Se puede activar en Ajustes.
    val isDynamicColorEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR_ENABLED] ?: false }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    /** La "carpeta principal": si está marcada, el tab de "Carpetas" abre directo su detalle. */
    val pinnedFolderId: Flow<Long?> =
        context.dataStore.data.map { it[Keys.PINNED_FOLDER_ID]?.toLongOrNull() }

    suspend fun setPinnedFolderId(id: Long?) {
        context.dataStore.edit {
            if (id == null) it.remove(Keys.PINNED_FOLDER_ID) else it[Keys.PINNED_FOLDER_ID] = id.toString()
        }
    }

    val defaultReminderOffsetsMinutes: Flow<List<Long>> = context.dataStore.data.map {
        listOf(
            (it[Keys.DEFAULT_REMINDER_WEEK_BEFORE_MIN] ?: (7 * 24 * 60)).toLong(),
            (it[Keys.DEFAULT_REMINDER_DAY_BEFORE_MIN] ?: (24 * 60)).toLong(),
        )
    }

    val timerChallenge: Flow<DismissChallengeType> = context.dataStore.data.map {
        runCatching { DismissChallengeType.valueOf(it[Keys.TIMER_CHALLENGE_TYPE] ?: "") }
            .getOrDefault(DismissChallengeType.NONE)
    }
    val timerChallengeDifficulty: Flow<Int> = context.dataStore.data.map { it[Keys.TIMER_CHALLENGE_DIFFICULTY] ?: 1 }

    suspend fun setTimerChallenge(type: DismissChallengeType, difficulty: Int) {
        context.dataStore.edit {
            it[Keys.TIMER_CHALLENGE_TYPE] = type.name
            it[Keys.TIMER_CHALLENGE_DIFFICULTY] = difficulty
        }
    }

    // MIUI no expone ninguna API para comprobar estos dos permisos, así que no hay forma
    // automática de saber si ya están activados; se deja que el propio usuario lo confirme a mano
    // después de revisarlo, en vez de mostrarlos siempre en ámbar como "no se pudo verificar".
    val isXiaomiAutoStartConfirmed: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.XIAOMI_AUTOSTART_CONFIRMED] ?: false }

    suspend fun setXiaomiAutoStartConfirmed(confirmed: Boolean) {
        context.dataStore.edit { it[Keys.XIAOMI_AUTOSTART_CONFIRMED] = confirmed }
    }

    val isXiaomiBackgroundPopupConfirmed: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.XIAOMI_BACKGROUND_POPUP_CONFIRMED] ?: false }

    suspend fun setXiaomiBackgroundPopupConfirmed(confirmed: Boolean) {
        context.dataStore.edit { it[Keys.XIAOMI_BACKGROUND_POPUP_CONFIRMED] = confirmed }
    }

    // Por defecto apagado (12h con AM/PM chico): es el formato que la mayoría espera ver de
    // entrada en un reloj/alarma; quien prefiera 24h lo puede activar en Ajustes.
    val use24HourFormat: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.USE_24_HOUR_FORMAT] ?: false }

    suspend fun setUse24HourFormat(enabled: Boolean) {
        context.dataStore.edit { it[Keys.USE_24_HOUR_FORMAT] = enabled }
    }

    /** Minutos que pospone una alarma el botón "Posponer" de la pantalla de alarma sonando. */
    val snoozeMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.SNOOZE_MINUTES] ?: 5 }

    suspend fun setSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.SNOOZE_MINUTES] = minutes }
    }

    /** Duración de la prórroga del botón "X minutos más" del overlay de bloqueo (Reels/TikTok). */
    val blockGraceMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.BLOCK_GRACE_MINUTES] ?: 5 }

    suspend fun setBlockGraceMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.BLOCK_GRACE_MINUTES] = minutes }
    }

    /**
     * Minutos de anticipación con los que avisar "tu próxima clase empieza en X min" (#144).
     * 0 = apagado (por defecto): es un aviso nuevo y opcional, no algo que todo el mundo quiera
     * de entrada como sí pasa con posponer/prórroga de bloqueo (que ya existían de antes).
     */
    val nextClassNotificationMinutesBefore: Flow<Int> =
        context.dataStore.data.map { it[Keys.NEXT_CLASS_NOTIFICATION_MINUTES] ?: 0 }

    suspend fun setNextClassNotificationMinutesBefore(minutes: Int) {
        context.dataStore.edit { it[Keys.NEXT_CLASS_NOTIFICATION_MINUTES] = minutes }
    }

    /**
     * Últimos hh/mm/ss que el usuario puso en el temporizador (en segundos totales), para que la
     * próxima vez que abra la pantalla ya aparezca ese mismo valor en vez de siempre "5 min" fijo.
     */
    val lastTimerDurationSeconds: Flow<Int> =
        context.dataStore.data.map { it[Keys.LAST_TIMER_DURATION_MILLIS] ?: (5 * 60) }

    suspend fun setLastTimerDurationSeconds(seconds: Int) {
        context.dataStore.edit { it[Keys.LAST_TIMER_DURATION_MILLIS] = seconds }
    }

    /** Aviso de "volumen de alarma bajo" (#9): activado por defecto, es un aviso de seguridad. */
    val lowAlarmVolumeWarningEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.LOW_ALARM_VOLUME_WARNING_ENABLED] ?: true }

    suspend fun setLowAlarmVolumeWarningEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.LOW_ALARM_VOLUME_WARNING_ENABLED] = enabled }
    }

    /** Con cuántas horas de anticipación avisar si hay una alarma por sonar y el volumen está bajo. */
    val lowAlarmVolumeWarningHoursAhead: Flow<Int> =
        context.dataStore.data.map { it[Keys.LOW_ALARM_VOLUME_WARNING_HOURS_AHEAD] ?: 12 }

    suspend fun setLowAlarmVolumeWarningHoursAhead(hours: Int) {
        context.dataStore.edit { it[Keys.LOW_ALARM_VOLUME_WARNING_HOURS_AHEAD] = hours }
    }

    /**
     * Últimas claves de permiso vistas como concedidas (ver [com.fritangui.wakeup.permissions.PermissionRevocationTracker]):
     * comparar contra esto es lo que permite detectar que uno se apagó (#8), en vez de solo saber
     * que "no está concedido" (que también pasa la primerísima vez, antes de configurar nada).
     */
    val lastKnownGrantedPermissionKeys: Flow<Set<String>> =
        context.dataStore.data.map { it[Keys.LAST_KNOWN_GRANTED_PERMISSIONS] ?: emptySet() }

    suspend fun setLastKnownGrantedPermissionKeys(keys: Set<String>) {
        context.dataStore.edit { it[Keys.LAST_KNOWN_GRANTED_PERMISSIONS] = keys }
    }

    /** Evita re-notificar el mismo "volumen bajo antes de tal alarma" una y otra vez cada hora. */
    val lastNotifiedLowVolumeTriggerMillis: Flow<Long> =
        context.dataStore.data.map { it[Keys.LAST_NOTIFIED_LOW_VOLUME_TRIGGER_MILLIS] ?: 0L }

    suspend fun setLastNotifiedLowVolumeTriggerMillis(millis: Long) {
        context.dataStore.edit { it[Keys.LAST_NOTIFIED_LOW_VOLUME_TRIGGER_MILLIS] = millis }
    }
}
