package com.fritangui.wakeup.data.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
        val LAST_SELECTED_FOLDER_ID = stringPreferencesKey("last_selected_folder_id")
        val DEFAULT_REMINDER_WEEK_BEFORE_MIN = intPreferencesKey("default_reminder_week_min")
        val DEFAULT_REMINDER_DAY_BEFORE_MIN = intPreferencesKey("default_reminder_day_min")
        val TIMER_CHALLENGE_TYPE = stringPreferencesKey("timer_challenge_type")
        val TIMER_CHALLENGE_DIFFICULTY = intPreferencesKey("timer_challenge_difficulty")
    }

    val isXiaomiOnboardingDone: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.XIAOMI_ONBOARDING_DONE] ?: false }

    suspend fun setXiaomiOnboardingDone(done: Boolean) {
        context.dataStore.edit { it[Keys.XIAOMI_ONBOARDING_DONE] = done }
    }

    val isDynamicColorEnabled: Flow<Boolean> =
        context.dataStore.data.map { it[Keys.DYNAMIC_COLOR_ENABLED] ?: true }

    suspend fun setDynamicColorEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DYNAMIC_COLOR_ENABLED] = enabled }
    }

    val lastSelectedFolderId: Flow<Long?> =
        context.dataStore.data.map { it[Keys.LAST_SELECTED_FOLDER_ID]?.toLongOrNull() }

    suspend fun setLastSelectedFolderId(id: Long?) {
        context.dataStore.edit {
            if (id == null) it.remove(Keys.LAST_SELECTED_FOLDER_ID) else it[Keys.LAST_SELECTED_FOLDER_ID] = id.toString()
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
}
