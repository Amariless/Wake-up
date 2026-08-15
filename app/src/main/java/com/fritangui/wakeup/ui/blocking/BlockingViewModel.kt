package com.fritangui.wakeup.ui.blocking

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.blocking.BlockableApp
import com.fritangui.wakeup.blocking.BlockableAppCatalog
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Antes esta pantalla dejaba crear/eliminar reglas libremente (FAB + diálogo). Ahora el catálogo de
 * apps bloqueables es fijo ([BlockableAppCatalog]) — cada fila de la UI es una app conocida que se
 * activa/desactiva con un switch, sin botones de añadir/eliminar (#154/#155): la primera vez que se
 * activa una fila se crea su [BlockRuleEntity] por debajo; desactivarla solo la apaga, no la borra
 * (para no perder el límite configurado).
 */
@HiltViewModel
class BlockingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val usageRepository: UsageRepository,
) : ViewModel() {

    data class Row(
        val app: BlockableApp,
        val rule: BlockRuleEntity?,
        val isInstalled: Boolean,
        val usedMinutes: Long,
    )

    // Se calcula una sola vez: si el usuario instala/desinstala una de estas apps mientras la
    // pantalla está abierta es un caso raro, y de todos modos se recalcula al reabrir la pantalla
    // (nuevo ViewModel).
    private val installedByPackage: Map<String, Boolean> = BlockableAppCatalog.apps.associate { app ->
        app.packageName to BlockableAppCatalog.packageAliases(app).any(::isPackageInstalled)
    }

    val rows: StateFlow<List<Row>> = combine(
        usageRepository.observeBlockRules(),
        usageRepository.observeSurfaceUsageForDay(todayEpochDay()),
    ) { rules, usage ->
        val usedMinutesBySurface = usage.associate { it.surface to it.accumulatedMillis / 60_000 }
        BlockableAppCatalog.apps.map { app ->
            Row(
                app = app,
                rule = rules.firstOrNull { it.surface == app.surface },
                isInstalled = installedByPackage[app.packageName] ?: false,
                usedMinutes = usedMinutesBySurface[app.surface] ?: 0L,
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setEnabled(row: Row, enabled: Boolean) {
        viewModelScope.launch {
            val rule = row.rule
            if (rule != null) {
                usageRepository.updateBlockRule(rule.copy(isEnabled = enabled))
            } else {
                usageRepository.upsertBlockRule(
                    BlockRuleEntity(
                        packageName = row.app.packageName,
                        surface = row.app.surface,
                        dailyLimitMinutes = row.app.defaultDailyLimitMinutes,
                        isEnabled = enabled,
                    ),
                )
            }
        }
    }

    fun updateLimit(row: Row, minutes: Int) {
        val clamped = minutes.coerceIn(1, 720)
        viewModelScope.launch {
            val rule = row.rule
            if (rule != null) {
                usageRepository.updateBlockRule(rule.copy(dailyLimitMinutes = clamped))
            } else {
                usageRepository.upsertBlockRule(
                    BlockRuleEntity(
                        packageName = row.app.packageName,
                        surface = row.app.surface,
                        dailyLimitMinutes = clamped,
                        isEnabled = false,
                    ),
                )
            }
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0)
        true
    }.getOrDefault(false)
}
