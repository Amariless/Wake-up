package com.fritangui.wakeup.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Los widgets también se refrescan solos cada 30 min (mínimo permitido por
 * Android, ver `next_classes_widget_info.xml`), pero para que se sientan al
 * día se llama a esto tras cualquier cambio relevante (guardar una tarea,
 * agregar/editar un horario de clase).
 */
@Singleton
class WidgetRefresher @Inject constructor(
    @ApplicationContext private val context: Context,
    private val widgetRefreshScheduler: WidgetRefreshScheduler,
) {
    suspend fun refreshAll() {
        NextClassesWidget().updateAll(context)
        NextTasksWidget().updateAll(context)
        ScreenTimeWidget().updateAll(context)
        // Reprograma la alarma del próximo cruce de horario (#154) cada vez que se refresca, sea
        // por qué motivo sea (edición de datos, refresco periódico, arranque de la app, o el
        // propio cruce disparándose) — así siempre queda armada para el cruce más reciente.
        widgetRefreshScheduler.scheduleNextBoundary()
    }
}
