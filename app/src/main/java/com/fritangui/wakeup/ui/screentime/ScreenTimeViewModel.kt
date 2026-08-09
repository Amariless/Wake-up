package com.fritangui.wakeup.ui.screentime

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.usage.ScreenTimeRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AppUsageRow(val packageName: String, val label: String, val minutes: Long)

private val DEFAULT_SOCIAL_PACKAGES = listOf(
    "com.instagram.android", "com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
    "com.twitter.android", "com.facebook.katana", "com.snapchat.android", "com.reddit.frontpage",
    "com.google.android.youtube",
)

@HiltViewModel
class ScreenTimeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
    private val screenTimeRefresher: ScreenTimeRefresher,
) : ViewModel() {

    private val packageManager: PackageManager = context.packageManager

    init {
        // El trabajo en segundo plano (cada ~15 min vía WorkManager) no es confiable en MIUI, así
        // que además se pide un refresco inmediato apenas se abre esta pantalla — si no, los datos
        // se sentían desactualizados porque solo se refrescaban cuando el sistema decidía correr
        // el Worker, que podía tardar mucho más de 15 min o no correr en absoluto.
        viewModelScope.launch { screenTimeRefresher.refreshNow() }
    }

    val todayUsage: StateFlow<List<AppUsageRow>> = usageRepository.observeForDay(todayEpochDay())
        .map { list ->
            list.sortedByDescending { it.minutesUsed }.map { entry ->
                AppUsageRow(entry.packageName, resolveLabel(entry.packageName), entry.minutesUsed)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alertRules: StateFlow<List<UsageAlertRuleEntity>> = usageRepository.observeAlertRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private fun resolveLabel(packageName: String): String = runCatching {
        val appInfo = packageManager.getApplicationInfo(packageName, 0)
        packageManager.getApplicationLabel(appInfo).toString()
    }.getOrDefault(packageName)

    fun addSocialMediaRule(dailyThresholdMinutes: Int) {
        viewModelScope.launch {
            usageRepository.upsertAlertRule(
                UsageAlertRuleEntity(
                    label = "Redes sociales",
                    packageNames = DEFAULT_SOCIAL_PACKAGES,
                    dailyThresholdMinutes = dailyThresholdMinutes,
                ),
            )
        }
    }

    fun setRuleEnabled(rule: UsageAlertRuleEntity, enabled: Boolean) {
        viewModelScope.launch { usageRepository.upsertAlertRule(rule.copy(isEnabled = enabled)) }
    }

    fun deleteRule(rule: UsageAlertRuleEntity) {
        viewModelScope.launch { usageRepository.deleteAlertRule(rule) }
    }
}
