package com.fritangui.wakeup.alarm

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.fritangui.wakeup.alarm.ui.TimerRingingScreen
import com.fritangui.wakeup.ui.theme.WakeUpTheme
import dagger.hilt.android.AndroidEntryPoint

/** Análoga a [AlarmRingingActivity] pero para cuando el temporizador llega a cero. */
@AndroidEntryPoint
class TimerRingingActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        configureShowOverLockScreen()
        setContent {
            WakeUpTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TimerRingingScreen(onDismissed = { finishRinging() })
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

    override fun onResume() {
        super.onResume()
        isVisible = true
    }

    override fun onPause() {
        super.onPause()
        isVisible = false
    }

    private fun finishRinging() {
        startService(TimerForegroundService.actionIntent(this, TimerForegroundService.ACTION_STOP_RINGING))
        finish()
    }

    override fun onDestroy() {
        isVisible = false
        super.onDestroy()
    }

    companion object {
        @Volatile
        var isVisible: Boolean = false
    }
}
