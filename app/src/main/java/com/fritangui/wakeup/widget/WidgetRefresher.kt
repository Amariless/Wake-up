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
        refreshClasses()
        NextTasksWidget().updateAll(context)
        ScreenTimeWidget().updateAll(context)
    }

    /**
     * Solo el widget de "próximas clases" (+ reprogramar el próximo cruce de horario, #154). Antes,
     * cada cruce de horario de clase (varias veces al día) disparaba una recomposición Glance
     * completa de los 3 widgets vía [refreshAll], aunque ningún dato de tareas o de uso de pantalla
     * hubiera cambiado.
     */
    suspend fun refreshClasses() {
        NextClassesWidget().updateAll(context)
        // Reprograma la alarma del próximo cruce de horario (#154) cada vez que se refresca, sea
        // por qué motivo sea (edición de datos, refresco periódico, arranque de la app, o el
        // propio cruce disparándose) — así siempre queda armada para el cruce más reciente.
        widgetRefreshScheduler.scheduleNextBoundary()
    }
}
