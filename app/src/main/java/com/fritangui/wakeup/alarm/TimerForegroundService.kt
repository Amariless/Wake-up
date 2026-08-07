package com.fritangui.wakeup.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TimerUiState(
    val totalMillis: Long = 0L,
    val remainingMillis: Long = 0L,
    val isRunning: Boolean = false,
    val isRinging: Boolean = false,
    val challenge: DismissChallengeType = DismissChallengeType.NONE,
    val difficulty: Int = 1,
)

/**
 * Temporizador general del reloj (fuera de cualquier carpeta). Igual que las
 * alarmas, corre en un foreground service para no perderse si la app pasa a
 * segundo plano, y al llegar a cero suena con el mismo mecanismo confiable
 * (wakelock + activity de pantalla completa + watchdog) que las alarmas.
 */
@AndroidEntryPoint
class TimerForegroundService : LifecycleService() {

    @Inject lateinit var notificationHelper: NotificationHelper

    private var tickJob: Job? = null
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private var watchdogTicks = 0

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START -> {
                val durationMillis = intent.getLongExtra(EXTRA_DURATION_MILLIS, 0L)
                val challenge = intent.getStringExtra(EXTRA_CHALLENGE)
                    ?.let { runCatching { DismissChallengeType.valueOf(it) }.getOrDefault(DismissChallengeType.NONE) }
                    ?: DismissChallengeType.NONE
                val difficulty = intent.getIntExtra(EXTRA_DIFFICULTY, 1)
                start(durationMillis, challenge, difficulty)
            }
            ACTION_PAUSE -> pause()
            ACTION_RESUME -> resume()
            ACTION_CANCEL -> cancelTimer()
            ACTION_STOP_RINGING -> stopRinging()
        }
        return START_STICKY
    }

    private fun start(durationMillis: Long, challenge: DismissChallengeType, difficulty: Int) {
        if (durationMillis <= 0) return
        _state.value = TimerUiState(
            totalMillis = durationMillis,
            remainingMillis = durationMillis,
            isRunning = true,
            challenge = challenge,
            difficulty = difficulty,
        )
        startForeground(
            NotificationHelper.TIMER_RUNNING_NOTIF_ID,
            notificationHelper.buildTimerRunningNotification(durationMillis, false, pausePendingIntent(), cancelPendingIntent()),
        )
        runTicker()
    }

    private fun pause() {
        val current = _state.value
        if (!current.isRunning) return
        tickJob?.cancel()
        _state.value = current.copy(isRunning = false)
        notificationHelper.notifyTimerRunning(current.remainingMillis, true, resumePendingIntent(), cancelPendingIntent())
    }

    private fun resume() {
        val current = _state.value
        if (current.isRunning || current.remainingMillis <= 0) return
        _state.value = current.copy(isRunning = true)
        runTicker()
    }

    private fun cancelTimer() {
        tickJob?.cancel()
        _state.value = TimerUiState()
        notificationHelper.cancelTimerRunningNotification()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun runTicker() {
        tickJob?.cancel()
        tickJob = lifecycleScope.launch {
            while (_state.value.isRunning && _state.value.remainingMillis > 0) {
                delay(500)
                val current = _state.value
                if (!current.isRunning) break
                val next = (current.remainingMillis - 500).coerceAtLeast(0)
                _state.value = current.copy(remainingMillis = next)
                notificationHelper.notifyTimerRunning(next, false, pausePendingIntent(), cancelPendingIntent())
                if (next <= 0L) {
                    startRinging()
                    break
                }
            }
        }
    }

    private fun startRinging() {
        val current = _state.value
        _state.value = current.copy(isRunning = false, isRinging = true, remainingMillis = 0)
        notificationHelper.cancelTimerRunningNotification()
        acquireWakeLock()

        val fullScreenPendingIntent = PendingIntent.getActivity(
            this,
            TIMER_ACTIVITY_REQUEST_CODE,
            Intent(this, TimerRingingActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        startForeground(
            NotificationHelper.TIMER_RINGING_NOTIF_ID,
            notificationHelper.buildTimerRingingNotification(fullScreenPendingIntent, stopRingingPendingIntent()),
        )
        startSoundAndVibration()
        launchRingingActivity()
        startWatchdog()
    }

    private fun launchRingingActivity() {
        val intent = Intent(this, TimerRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        runCatching { startActivity(intent) }
    }

    private fun startWatchdog() {
        watchdogRunnable = object : Runnable {
            override fun run() {
                if (!_state.value.isRinging) return
                if (!TimerRingingActivity.isVisible) launchRingingActivity()
                watchdogTicks++
                val delayMs = if (watchdogTicks < 10) 3_000L else 12_000L
                watchdogHandler.postDelayed(this, delayMs)
            }
        }
        watchdogHandler.postDelayed(watchdogRunnable!!, 3_000L)
    }

    private fun startSoundAndVibration() {
        runCatching {
            val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getValidRingtoneUri(this)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@TimerForegroundService, uri!!)
                isLooping = true
                prepare()
                start()
            }
        }
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService<Vibrator>()
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 800, 500), 0))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "WakeUp:TimerRingingWakeLock")
            .apply { acquire(10 * 60 * 1000L) }
    }

    private fun stopRinging() {
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        runCatching { wakeLock?.release() }
        wakeLock = null
        notificationHelper.cancelTimerRinging()
        _state.value = TimerUiState()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun pausePendingIntent() = servicePendingIntent(ACTION_PAUSE, 1)
    private fun resumePendingIntent() = servicePendingIntent(ACTION_RESUME, 2)
    private fun cancelPendingIntent() = servicePendingIntent(ACTION_CANCEL, 3)
    private fun stopRingingPendingIntent() = servicePendingIntent(ACTION_STOP_RINGING, 4)

    private fun servicePendingIntent(action: String, requestCode: Int): PendingIntent = PendingIntent.getService(
        this,
        requestCode,
        Intent(this, TimerForegroundService::class.java).apply { this.action = action },
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
    )

    override fun onDestroy() {
        tickJob?.cancel()
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        runCatching { mediaPlayer?.release() }
        runCatching { wakeLock?.release() }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.fritangui.wakeup.action.TIMER_START"
        const val ACTION_PAUSE = "com.fritangui.wakeup.action.TIMER_PAUSE"
        const val ACTION_RESUME = "com.fritangui.wakeup.action.TIMER_RESUME"
        const val ACTION_CANCEL = "com.fritangui.wakeup.action.TIMER_CANCEL"
        const val ACTION_STOP_RINGING = "com.fritangui.wakeup.action.TIMER_STOP_RINGING"
        const val EXTRA_DURATION_MILLIS = "extra_duration_millis"
        const val EXTRA_CHALLENGE = "extra_challenge"
        const val EXTRA_DIFFICULTY = "extra_difficulty"

        private const val TIMER_ACTIVITY_REQUEST_CODE = 60_102

        private val _state = MutableStateFlow(TimerUiState())
        val state: StateFlow<TimerUiState> = _state.asStateFlow()

        fun startIntent(context: Context, durationMillis: Long, challenge: DismissChallengeType, difficulty: Int) =
            Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_DURATION_MILLIS, durationMillis)
                putExtra(EXTRA_CHALLENGE, challenge.name)
                putExtra(EXTRA_DIFFICULTY, difficulty)
            }

        fun actionIntent(context: Context, action: String) =
            Intent(context, TimerForegroundService::class.java).apply { this.action = action }
    }
}
