package com.fritangui.wakeup.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Se dispara justo cuando empieza o termina una clase (ver [WidgetRefreshScheduler]): refresca
 * los widgets ahí mismo — [WidgetRefresher.refreshAll] ya se encarga de reprogramar el siguiente
 * cruce al final — para que "Próximas clases" no dependa solo del refresco periódico de ~30 min
 * de Android (#154).
 */
@AndroidEntryPoint
class WidgetRefreshReceiver : BroadcastReceiver() {

    @Inject lateinit var widgetRefresher: WidgetRefresher

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                widgetRefresher.refreshAll()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
