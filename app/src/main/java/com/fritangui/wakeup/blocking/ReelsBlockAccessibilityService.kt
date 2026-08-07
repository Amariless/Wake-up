package com.fritangui.wakeup.blocking

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detecta cuándo el usuario está en Reels de Instagram (o el feed de TikTok)
 * inspeccionando los resource-id de los nodos de accesibilidad — ver
 * [ReelsNodeDetector] — y cuenta el tiempo contra el límite diario configurado
 * en [BlockRuleEntity]. Nunca actúa sobre los DMs: [ReelsNodeDetector] excluye
 * explícitamente esas pantallas.
 */
@AndroidEntryPoint
class ReelsBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var usageRepository: UsageRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rulesWatcherJob: Job? = null
    private var cachedRules: List<BlockRuleEntity> = emptyList()

    private var currentSurface: BlockSurface? = null
    private var surfaceStartedAtElapsedMs: Long = 0L
    private var lastBlockTriggerAtElapsedMs: Long = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        rulesWatcherJob = scope.launch {
            usageRepository.observeBlockRules().collect { cachedRules = it }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return
        val root = rootInActiveWindow ?: return
        val result = ReelsNodeDetector.detect(root, packageName)
        _lastDetection.value = DetectionDebugInfo(packageName, result.surface, result.matchedIds)

        if (result.surface != currentSurface) {
            flushAccumulatedTime()
            currentSurface = result.surface
            surfaceStartedAtElapsedMs = SystemClock.elapsedRealtime()
        }

        result.surface?.let { checkLimit(it) }
    }

    /** Persiste en Room el tiempo acumulado desde que se entró a la superficie actual. */
    private fun flushAccumulatedTime() {
        val surface = currentSurface ?: return
        val elapsed = SystemClock.elapsedRealtime() - surfaceStartedAtElapsedMs
        if (elapsed <= 0) return
        scope.launch {
            usageRepository.addSurfaceUsageMillis(todayEpochDay(), surface, elapsed)
        }
    }

    private fun checkLimit(surface: BlockSurface) {
        val rule = cachedRules.firstOrNull { it.surface == surface && it.isEnabled } ?: return
        // Evita disparar el overlay en cada evento: como mucho una vez cada 10s.
        val now = SystemClock.elapsedRealtime()
        if (now - lastBlockTriggerAtElapsedMs < 10_000) return

        scope.launch {
            val persisted = usageRepository.getSurfaceUsageMillis(todayEpochDay(), surface)
            val inProgress = now - surfaceStartedAtElapsedMs
            val totalMinutes = (persisted + inProgress) / 60_000.0
            if (totalMinutes >= rule.dailyLimitMinutes) {
                lastBlockTriggerAtElapsedMs = now
                triggerBlock(surface)
            }
        }
    }

    private fun triggerBlock(surface: BlockSurface) {
        performGlobalAction(GLOBAL_ACTION_BACK)
        val label = when (surface) {
            BlockSurface.INSTAGRAM_REELS -> "Reels"
            BlockSurface.TIKTOK_FOR_YOU -> "TikTok"
            BlockSurface.GENERIC_APP_TIME_LIMIT -> "esta app"
        }
        startService(BlockOverlayService.intent(this, label))
    }

    override fun onInterrupt() {
        // requerido por AccessibilityService; no hay estado que limpiar aquí.
    }

    override fun onDestroy() {
        flushAccumulatedTime()
        rulesWatcherJob?.cancel()
        _isRunning.value = false
        super.onDestroy()
    }

    /** Info de depuración expuesta para [NodeInspectorScreen], útil si Instagram cambia sus ids. */
    data class DetectionDebugInfo(val packageName: String, val surface: BlockSurface?, val matchedIds: Set<String>)

    companion object {
        private val _isRunning = MutableStateFlow(false)
        val isRunning: StateFlow<Boolean> = _isRunning.asStateFlow()

        private val _lastDetection = MutableStateFlow<DetectionDebugInfo?>(null)
        val lastDetection: StateFlow<DetectionDebugInfo?> = _lastDetection.asStateFlow()
    }
}
