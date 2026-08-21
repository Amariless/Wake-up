package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.fritangui.wakeup.widget.WidgetRefreshScheduler
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * AlarmManager olvida TODAS las alarmas al reiniciar el teléfono: este receiver
 * las vuelve a programar todas (alarmas activas + recordatorios de tareas
 * pendientes de carpetas activas, más la del próximo cruce de horario para los
 * widgets, #154) apenas el sistema termina de arrancar.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var alarmController: AlarmController
    @Inject lateinit var widgetRefreshScheduler: WidgetRefreshScheduler

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") {
            return
        }
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                alarmController.rescheduleEverything()
                widgetRefreshScheduler.scheduleNextBoundary()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
