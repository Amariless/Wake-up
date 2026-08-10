package com.fritangui.wakeup.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.todayIn
import javax.inject.Inject

/** Envoltorio delgado sobre [UsageStatsManager] (requiere el permiso especial "Acceso a datos de uso"). */
class SystemUsageStatsSource @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    /**
     * @return minutos de uso en primer plano hoy, por paquete.
     *
     * OJO: esto NO usa `queryUsageStats(INTERVAL_BEST, ...)` a propósito. Esa función devuelve el
     * total acumulado del "bucket" (día/semana/mes) que el sistema decide que mejor cubre el rango
     * pedido, y ese total NO viene recortado al rango exacto que se pidió — si Android elige un
     * bucket que arranca antes de hoy (algo que hace con más frecuencia de la que uno esperaría,
     * sobre todo si no hay datos "limpios" para exactamente hoy), el número incluye uso de días
     * anteriores completos. Así se explicaba que la app dijera "5h hoy" a las 3 de la mañana, o que
     * Wake up (usada intensamente en sesiones de prueba de días distintos) apareciera con más
     * tiempo que Instagram.
     *
     * En vez de eso, se recorren los eventos crudos (`queryEvents`) y se suma manualmente cuánto
     * duró cada tramo en primer plano, recortando cualquier tramo que haya empezado antes de la
     * medianoche de hoy — así el total sí queda exactamente acotado al rango pedido, igual que hace
     * el propio "Tiempo de pantalla" de Android/MIUI.
     */
    fun queryTodayUsageMinutesByPackage(): Map<String, Long> {
        val manager = context.getSystemService<UsageStatsManager>() ?: return emptyMap()
        val zone = TimeZone.currentSystemDefault()
        val startOfDayMillis = Clock.System.todayIn(zone).atStartOfDayIn(zone).toEpochMilliseconds()
        val nowMillis = System.currentTimeMillis()

        val events = runCatching { manager.queryEvents(startOfDayMillis, nowMillis) }.getOrNull() ?: return emptyMap()

        val totalsMillis = mutableMapOf<String, Long>()
        // Por si alguna app quedó en primer plano sin su evento de "pasó a segundo plano" todavía
        // dentro del rango consultado (lo normal para la app que está abierta ahora mismo).
        val foregroundSinceMillis = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            when (event.eventType) {
                // MOVE_TO_FOREGROUND/BACKGROUND y ACTIVITY_RESUMED/PAUSED comparten los mismos
                // valores enteros (1 y 2) — cubre tanto versiones viejas como nuevas de Android.
                UsageEvents.Event.MOVE_TO_FOREGROUND -> foregroundSinceMillis[pkg] = event.timeStamp
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val startedAt = foregroundSinceMillis.remove(pkg) ?: continue
                    val clippedStart = startedAt.coerceAtLeast(startOfDayMillis)
                    val duration = (event.timeStamp - clippedStart).coerceAtLeast(0L)
                    totalsMillis[pkg] = (totalsMillis[pkg] ?: 0L) + duration
                }
            }
        }

        // Lo que sigue en primer plano al momento de la consulta: cuenta hasta ahora mismo.
        foregroundSinceMillis.forEach { (pkg, startedAt) ->
            val clippedStart = startedAt.coerceAtLeast(startOfDayMillis)
            val duration = (nowMillis - clippedStart).coerceAtLeast(0L)
            totalsMillis[pkg] = (totalsMillis[pkg] ?: 0L) + duration
        }

        return totalsMillis.mapValues { it.value / 60_000 }
    }
}
