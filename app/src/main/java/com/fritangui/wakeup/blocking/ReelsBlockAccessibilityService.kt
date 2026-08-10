package com.fritangui.wakeup.blocking

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import com.fritangui.wakeup.data.db.entity.BlockRuleEntity
import com.fritangui.wakeup.data.db.entity.BlockSurface
import com.fritangui.wakeup.data.db.entity.UsageAlertRuleEntity
import com.fritangui.wakeup.data.repository.UsageRepository
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.notifications.NotificationHelper
import com.fritangui.wakeup.usage.ScreenTimeRefresher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration

/**
 * Detecta cuándo el usuario está en Reels de Instagram (o el feed de TikTok)
 * inspeccionando los resource-id de los nodos de accesibilidad — ver
 * [ReelsNodeDetector] — y cuenta el tiempo contra el límite diario configurado
 * en [BlockRuleEntity]. Nunca actúa sobre los DMs: [ReelsNodeDetector] excluye
 * explícitamente esas pantallas.
 *
 * También dispara el aviso de "llevas X min en redes sociales" ([checkAlertRules]): a diferencia
 * del bloqueo de Reels/TikTok (que aplica solo a esos dos paquetes), un [UsageAlertRuleEntity]
 * puede cubrir cualquier conjunto de apps, así que el servicio escucha eventos de TODAS las apps
 * (ver accessibility_service_config.xml) para saber cuándo se entra a alguna de ellas.
 */
@AndroidEntryPoint
class ReelsBlockAccessibilityService : AccessibilityService() {

    @Inject lateinit var usageRepository: UsageRepository
    @Inject lateinit var screenTimeRefresher: ScreenTimeRefresher
    @Inject lateinit var notificationHelper: NotificationHelper

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rulesWatcherJob: Job? = null
    private var alertRulesWatcherJob: Job? = null
    private var commandsJob: Job? = null
    private var cachedRules: List<BlockRuleEntity> = emptyList()
    private var cachedAlertRules: List<UsageAlertRuleEntity> = emptyList()

    private var currentSurface: BlockSurface? = null
    private var surfaceStartedAtElapsedMs: Long = 0L
    private var lastBlockTriggerAtElapsedMs: Long = 0L
    /** "Botón 5 minutos más" del overlay: deja pasar el límite de esa superficie hasta este instante. */
    private val snoozedUntilElapsedMs = mutableMapOf<BlockSurface, Long>()

    // El tiempo solo se guardaba en Room al detectar que se salió de la superficie (cambio de app o
    // de pestaña). Si en cambio la app se cerraba de golpe (deslizarla fuera de Recientes, matar el
    // proceso) nunca llegaba ese evento de "salida" y el tramo en curso se perdía entero — cerrar y
    // volver a abrir Instagram así "reseteaba" el conteo real y dejaba saltarse el límite. Con este
    // flush periódico, como mucho se pierden unos segundos en vez de sesiones completas.
    private var lastPeriodicFlushAtElapsedMs: Long = 0L

    // Detección de "se salió de la superficie en prórroga" con un poco de margen: el detector de
    // Reels es una heurística (ver ReelsNodeDetector) y puede parpadear un instante entre estados
    // parecidos — sin este margen, el velo gris de "5 minutos más" se quitaba solo a los pocos
    // eventos de haberlo activado aunque el usuario siguiera ahí.
    private var leaveDebounceJob: Job? = null

    // Aviso de "llevas X min en redes sociales": se revisa como mucho cada ALERT_RECHECK_INTERVAL_MS
    // (llegan muchos eventos de accesibilidad por segundo, no hace falta comprobar en cada uno) y
    // como mucho una vez por regla por día (si no, reabrir la app después de ya haber avisado hoy
    // volvería a notificar cada vez).
    private var lastAlertCheckAtElapsedMs: Long = 0L
    private val notifiedRuleIdsToday = mutableMapOf<Long, Long>() // ruleId -> epochDay en que se avisó

    override fun onServiceConnected() {
        super.onServiceConnected()
        _isRunning.value = true
        rulesWatcherJob = scope.launch {
            usageRepository.observeBlockRules().collect { cachedRules = it }
        }
        alertRulesWatcherJob = scope.launch {
            usageRepository.observeAlertRules().collect { cachedAlertRules = it }
        }
        commandsJob = scope.launch {
            commands.collect { command ->
                when (command) {
                    is Command.GoHome -> performGlobalAction(GLOBAL_ACTION_HOME)
                    is Command.Snooze -> snoozedUntilElapsedMs[command.surface] =
                        SystemClock.elapsedRealtime() + command.duration.inWholeMilliseconds
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        val packageName = event.packageName?.toString() ?: return

        // El recorrido de nodos de Reels/TikTok (hasta 500 nodos) solo hace falta, y solo es
        // preciso, para esos dos paquetes: evita hacerlo para cualquier otra app ahora que el
        // servicio escucha eventos de todas (necesario para checkAlertRules, más abajo).
        if (packageName == "com.instagram.android" || packageName == "com.zhiliaoapp.musically" || packageName == "com.ss.android.ugc.trill") {
            val root = rootInActiveWindow
            val result = if (root != null) ReelsNodeDetector.detect(root, packageName) else ReelsNodeDetector.DetectionResult(null, emptySet())
            _lastDetection.value = DetectionDebugInfo(packageName, result.surface, result.matchedIds)

            if (result.surface != currentSurface) {
                scheduleLeaveCheckIfSnoozed(currentSurface)
                flushAccumulatedTime()
                currentSurface = result.surface
                surfaceStartedAtElapsedMs = SystemClock.elapsedRealtime()
            } else {
                result.surface?.let { periodicFlush(it) }
            }
            result.surface?.let { checkLimit(it) }
        } else if (currentSurface != null) {
            // Se salió de Instagram/TikTok hacia otra app: cierra el tramo que se venía acumulando.
            scheduleLeaveCheckIfSnoozed(currentSurface)
            flushAccumulatedTime()
            currentSurface = null
        }

        checkAlertRules(packageName)
    }

    /**
     * Si [surface] tenía la prórroga de "5 minutos más" activa y el usuario PARECE haber salido de
     * ella (cambió de superficie o cerró la app), espera un momento antes de avisarle a
     * [BlockOverlayService] que quite el velo — no al toque, porque el detector de Reels es una
     * heurística que puede confundirse un instante entre pantallas parecidas (ver
     * ReelsNodeDetector), y disparar el aviso en cada parpadeo hacía que el velo se quitara solo
     * casi de inmediato después de activarlo. Si para cuando pasa ese margen [currentSurface] ya
     * volvió a ser la misma superficie, no se avisa nada (era solo ruido de detección).
     */
    private fun scheduleLeaveCheckIfSnoozed(surface: BlockSurface?) {
        if (surface == null) return
        val snoozedUntil = snoozedUntilElapsedMs[surface] ?: return
        if (SystemClock.elapsedRealtime() >= snoozedUntil) return
        leaveDebounceJob?.cancel()
        leaveDebounceJob = scope.launch {
            delay(LEAVE_DEBOUNCE_MS)
            if (currentSurface != surface) {
                BlockOverlayService.requestDismissGrace()
            }
        }
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

    /**
     * Igual que [flushAccumulatedTime] pero sin esperar a que se salga de la superficie: se llama
     * como mucho cada [PERIODIC_FLUSH_INTERVAL_MS] mientras se sigue en la misma, y reinicia el
     * punto de partida — así, si el usuario cierra la app de un tirón (deslizarla en Recientes),
     * como mucho se pierden unos segundos de conteo en vez de la sesión completa.
     */
    private fun periodicFlush(surface: BlockSurface) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastPeriodicFlushAtElapsedMs < PERIODIC_FLUSH_INTERVAL_MS) return
        lastPeriodicFlushAtElapsedMs = now
        val elapsed = now - surfaceStartedAtElapsedMs
        if (elapsed <= 0) return
        scope.launch {
            usageRepository.addSurfaceUsageMillis(todayEpochDay(), surface, elapsed)
        }
        surfaceStartedAtElapsedMs = now
    }

    private fun checkLimit(surface: BlockSurface) {
        val rule = cachedRules.firstOrNull { it.surface == surface && it.isEnabled } ?: return
        val now = SystemClock.elapsedRealtime()
        // "5 minutos más" del overlay: no volver a bloquear hasta que pase la prórroga.
        if (now < (snoozedUntilElapsedMs[surface] ?: 0L)) return
        // Evita disparar el overlay en cada evento: como mucho una vez cada 10s.
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
        startService(BlockOverlayService.intent(this, label, surface))
    }

    /**
     * Aviso de "llevas X min hoy en redes sociales": antes vivía en [ScreenTimeRefresher] y se
     * revisaba cada vez que corría el worker periódico (~15 min) o se abría Bienestar, así que
     * podía saltar en cualquier momento del día sin relación con estar usando esa app. Ahora se
     * revisa justo cuando se entra (o se sigue) en una app cubierta por alguna regla, así que solo
     * aparece "al abrir una red social ya pasado el límite" o "mientras la sigues usando, justo al
     * cruzar el límite".
     */
    private fun checkAlertRules(packageName: String) {
        if (cachedAlertRules.isEmpty()) return
        val matchingRules = cachedAlertRules.filter { it.isEnabled && packageName in it.packageNames }
        if (matchingRules.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastAlertCheckAtElapsedMs < ALERT_RECHECK_INTERVAL_MS) return
        lastAlertCheckAtElapsedMs = now

        scope.launch {
            // Refresca el total de hoy contra UsageStatsManager antes de comparar: así el número es
            // el de ahora mismo, no el de la última vez que corrió el refresco en segundo plano.
            screenTimeRefresher.refreshNow()
            val today = todayEpochDay()
            matchingRules.forEach { rule ->
                if (notifiedRuleIdsToday[rule.id] == today) return@forEach
                val total = usageRepository.minutesUsedTodayFor(today, rule.packageNames)
                if (total >= rule.dailyThresholdMinutes) {
                    notifiedRuleIdsToday[rule.id] = today
                    notificationHelper.notifyUsageThreshold(rule.label, total, rule.dailyThresholdMinutes)
                }
            }
        }
    }

    override fun onInterrupt() {
        // requerido por AccessibilityService; no hay estado que limpiar aquí.
    }

    override fun onDestroy() {
        flushAccumulatedTime()
        rulesWatcherJob?.cancel()
        alertRulesWatcherJob?.cancel()
        commandsJob?.cancel()
        leaveDebounceJob?.cancel()
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

        private const val ALERT_RECHECK_INTERVAL_MS = 20_000L
        private const val PERIODIC_FLUSH_INTERVAL_MS = 15_000L
        private const val LEAVE_DEBOUNCE_MS = 2_500L

        // El overlay de bloqueo (otro proceso/componente, no un AccessibilityService) no puede
        // llamar directo a performGlobalAction ni tocar el estado de instancia de este servicio;
        // manda estos "comandos" por acá y la instancia en ejecución (si hay una) los atiende.
        private val _commands = MutableSharedFlow<Command>(extraBufferCapacity = 4)
        private val commands: SharedFlow<Command> = _commands.asSharedFlow()

        /** Manda al usuario a la pantalla de inicio: lo más parecido a "cerrar la app" sin permisos de sistema/root. */
        fun requestGoHome() {
            _commands.tryEmit(Command.GoHome)
        }

        /** Deja pasar el límite de [surface] durante [duration] (botón "5 minutos más" del overlay). */
        fun snooze(surface: BlockSurface, duration: Duration) {
            _commands.tryEmit(Command.Snooze(surface, duration))
        }

        private sealed interface Command {
            data object GoHome : Command
            data class Snooze(val surface: BlockSurface, val duration: Duration) : Command
        }
    }
}
