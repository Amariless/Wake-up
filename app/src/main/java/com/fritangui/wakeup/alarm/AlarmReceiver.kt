package com.fritangui.wakeup.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID

/**
 * Disparado por AlarmManager exactamente a la hora de la alarma. Debe ser lo más
 * ligero posible (los BroadcastReceiver solo tienen unos segundos antes de que
 * el sistema los considere "ANR"): su único trabajo es arrancar el foreground
 * service que de verdad hace sonar la alarma y mantiene todo vivo.
 */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId < 0) return

        val serviceIntent = Intent(context, RingingForegroundService::class.java).apply {
            action = RingingForegroundService.ACTION_START_RINGING
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        ContextCompat.startForegroundService(context, serviceIntent)
    }
}
