package com.fritangui.wakeup.ui.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val usageRepository: UsageRepository,
) : ViewModel() {

    val rules: StateFlow<List<BlockRuleEntity>> = usageRepository.observeBlockRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val todayUsageMinutes: StateFlow<Map<BlockSurface, Long>> = usageRepository
        .observeSurfaceUsageForDay(todayEpochDay())
        .map { list -> list.associate { it.surface to it.accumulatedMillis / 60_000 } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    fun addRule(surface: BlockSurface, dailyLimitMinutes: Int) {
        viewModelScope.launch {
            val packageName = when (surface) {
                BlockSurface.INSTAGRAM_REELS -> "com.instagram.android"
                BlockSurface.TIKTOK_FOR_YOU -> "com.zhiliaoapp.musically"
                BlockSurface.GENERIC_APP_TIME_LIMIT -> ""
            }
            usageRepository.upsertBlockRule(
                BlockRuleEntity(packageName = packageName, surface = surface, dailyLimitMinutes = dailyLimitMinutes),
            )
        }
    }

    fun setEnabled(rule: BlockRuleEntity, enabled: Boolean) {
        viewModelScope.launch { usageRepository.updateBlockRule(rule.copy(isEnabled = enabled)) }
    }

    fun updateLimit(rule: BlockRuleEntity, minutes: Int) {
        viewModelScope.launch { usageRepository.updateBlockRule(rule.copy(dailyLimitMinutes = minutes)) }
    }

    fun delete(rule: BlockRuleEntity) {
        viewModelScope.launch { usageRepository.deleteBlockRule(rule) }
    }
}
