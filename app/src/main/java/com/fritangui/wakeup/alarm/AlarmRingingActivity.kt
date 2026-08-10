package com.fritangui.wakeup.alarm

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.alarm.ui.AlarmRingingScreen
import com.fritangui.wakeup.ui.theme.WakeUpTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Activity dedicada exclusivamente a mostrar la alarma sonando. Vive fuera del
 * grafo de navegación principal a propósito: necesita mostrarse sobre la
 * pantalla de bloqueo y sobrevivir a que [RingingForegroundService] la
 * relance si el usuario navega fuera sin completar el reto de apagado.
 */
@AndroidEntryPoint
class AlarmRingingActivity : ComponentActivity() {

    @Inject lateinit var alarmScheduler: AlarmScheduler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureShowOverLockScreen()

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        setContent {
            WakeUpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AlarmRingingScreen(
                        alarmId = alarmId,
                        onDismissed = { finishRinging() },
                        onSnoozed = { minutes ->
                            alarmScheduler.scheduleSnooze(alarmId, minutes)
                            finishRinging()
                        },
                    )
                }
            }
        }
    }

    private fun configureShowOverLockScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON,
            )
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        isVisible = true
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
    }

    private fun finishRinging() {
        startService(RingingForegroundService.stopIntent(this))
        finish()
    }

    override fun onDestroy() {
        isVisible = false
        super.onDestroy()
    }

    companion object {
        /** Consultado por el watchdog de [RingingForegroundService] para saber si debe relanzarla. */
        @Volatile
        var isVisible: Boolean = false
    }
}
