package com.fritangui.wakeup.ui.screentime

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.fritangui.wakeup.data.db.entity.AppUsageDailyEntity
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.usage.ScreenTimeRefresher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import javax.inject.Inject

data class AppUsageRow(val packageName: String, val label: String, val minutes: Long)

/** [dayLabel] ya viene formateado ("Lu", "Ma"...) para no meter lógica de fechas en la UI. */
data class DayUsage(
    val epochDay: Long,
    val dayLabel: String,
    /** Nombre completo del día ("Lunes"), para lectores de pantalla — "Lu"/"Ma" es ambiguo sin apoyo visual. */
    val dayLabelFull: String,
    val totalMinutes: Long,
    val isToday: Boolean,
)

private val DIA_CORTO = listOf("Lu", "Ma", "Mi", "Ju", "Vi", "Sa", "Do")
private val DIA_LARGO = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

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

    /** Independiente de qué semana/mes se esté navegando abajo (#161): "Hoy" siempre compara
     *  contra el ayer real, no contra un día cualquiera del período que se esté mirando. */
    val yesterdayTotalMinutes: StateFlow<Long> = usageRepository.observeForDay(todayEpochDay() - 1)
        .map { list -> list.sumOf { it.minutesUsed } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    // 0 = semana/mes actual, -1 = la anterior, etc. — no se deja ir a futuro (ver [previousWeek]/
    // [nextWeek] y sus pares de mes), así que siempre es 0 o negativo (#161: antes "Últimos 7 días"
    // era fijo, sin forma de ver historial).
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset

    private val _monthOffset = MutableStateFlow(0)
    val monthOffset: StateFlow<Int> = _monthOffset

    /** La semana (lunes a domingo) según [weekOffset], para el gráfico de barras y su promedio. */
    val weeklyUsage: StateFlow<List<DayUsage>> = _weekOffset
        .flatMapLatest { offset ->
            val monday = mondayEpochDayForWeekOffset(offset)
            usageRepository.observeForRange(monday, monday + 6).map { entries -> dayUsageRange(entries, monday, monday + 6) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** El mes calendario según [monthOffset], para la vista "Mes" del mismo gráfico. */
    val monthlyUsage: StateFlow<List<DayUsage>> = _monthOffset
        .flatMapLatest { offset ->
            val today = LocalDate.fromEpochDays(todayEpochDay().toInt())
            val firstOfMonth = monthStart(today.year, today.monthNumber, offset)
            val firstOfNextMonth = monthStart(today.year, today.monthNumber, offset + 1)
            val fromDay = firstOfMonth.toEpochDays().toLong()
            val toDay = firstOfNextMonth.toEpochDays().toLong() - 1
            usageRepository.observeForRange(fromDay, toDay).map { entries -> dayUsageRange(entries, fromDay, toDay) }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val alertRules: StateFlow<List<UsageAlertRuleEntity>> = usageRepository.observeAlertRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun previousWeek() { _weekOffset.value -= 1 }
    fun nextWeek() { if (_weekOffset.value < 0) _weekOffset.value += 1 }
    fun previousMonth() { _monthOffset.value -= 1 }
    fun nextMonth() { if (_monthOffset.value < 0) _monthOffset.value += 1 }

    private fun dayUsageRange(entries: List<AppUsageDailyEntity>, fromDay: Long, toDay: Long): List<DayUsage> {
        val totalsByDay = entries.groupBy { it.dateEpochDay }.mapValues { (_, rows) -> rows.sumOf { it.minutesUsed } }
        val today = todayEpochDay()
        return (fromDay..toDay).map { day ->
            val isoDayOfWeek = LocalDate.fromEpochDays(day.toInt()).dayOfWeek.value
            DayUsage(
                epochDay = day,
                dayLabel = DIA_CORTO[isoDayOfWeek - 1],
                dayLabelFull = DIA_LARGO[isoDayOfWeek - 1],
                totalMinutes = totalsByDay[day] ?: 0L,
                isToday = day == today,
            )
        }
    }

    /** Lunes (ISO) de la semana [offset] semanas antes/después de la actual. */
    private fun mondayEpochDayForWeekOffset(offset: Int): Long {
        val today = LocalDate.fromEpochDays(todayEpochDay().toInt())
        val mondayThisWeek = today.plus(-(today.dayOfWeek.value - 1), DateTimeUnit.DAY)
        return mondayThisWeek.plus(offset * 7, DateTimeUnit.DAY).toEpochDays().toLong()
    }

    /** Primer día del mes [offsetMonths] meses antes/después de baseYear/baseMonth1To12 — con
     *  aritmética entera propia (sin DateTimeUnit.MONTH) para no depender de un caso menos probado. */
    private fun monthStart(baseYear: Int, baseMonth1To12: Int, offsetMonths: Int): LocalDate {
        val totalMonths = baseYear * 12 + (baseMonth1To12 - 1) + offsetMonths
        val year = totalMonths.floorDiv(12)
        val month = totalMonths.mod(12) + 1
        return LocalDate(year, month, 1)
    }

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
