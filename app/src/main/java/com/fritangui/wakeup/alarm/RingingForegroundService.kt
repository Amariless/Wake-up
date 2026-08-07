package com.fritangui.wakeup.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
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
import com.fritangui.wakeup.alarm.AlarmConstants.EXTRA_ALARM_ID
import com.fritangui.wakeup.data.repository.AlarmRepository
import com.fritangui.wakeup.notifications.NotificationHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * El corazón de la fiabilidad de las alarmas. Se arranca en primer plano
 * (`startForeground`) en cuanto llega el disparo de [AlarmReceiver] y se
 * mantiene viva con un WakeLock + un "watchdog" que relanza
 * [AlarmRingingActivity] si el usuario navega fuera de ella sin resolver el
 * reto de apagado. Esto es justo lo que evita el bug de apps como "Shake it"
 * de quedarse sonando en segundo plano sin forma fácil de volver a abrirlas:
 * aquí la propia alarma se reabre sola.
 *
 * Solo se detiene cuando el reto de apagado se completa (o cuando llega la
 * acción de "apagar solo esta vez" desde la notificación previa).
 */
@AndroidEntryPoint
class RingingForegroundService : LifecycleService() {

    @Inject lateinit var alarmRepository: AlarmRepository
    @Inject lateinit var alarmScheduler: AlarmScheduler
    @Inject lateinit var notificationHelper: NotificationHelper

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val watchdogHandler = Handler(Looper.getMainLooper())
    private var watchdogRunnable: Runnable? = null
    private var watchdogTicks = 0
    private var ringingAlarmId: Long? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_RINGING -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId >= 0 && ringingAlarmId == null) startRinging(alarmId)
            }
            ACTION_STOP_RINGING -> stopRinging()
        }
        return START_STICKY
    }

    private fun startRinging(alarmId: Long) {
        ringingAlarmId = alarmId
        acquireWakeLock()

        lifecycleScope.launch {
            val alarm = alarmRepository.getById(alarmId)
            if (alarm == null) {
                stopSelf()
                return@launch
            }

            if (alarm.skipNextOccurrence) {
                alarmRepository.setSkipNext(alarmId, false)
                alarmScheduler.scheduleAlarm(alarm.copy(skipNextOccurrence = false))
                stopSelf()
                return@launch
            }

            // Rearmar la próxima ocurrencia YA (antes de que el usuario interactúe):
            // si el teléfono se queda sin batería mientras suena, la de mañana igual queda programada.
            alarmScheduler.scheduleAlarm(alarm)
            if (alarm.repeatDaysBitmask == 0) {
                alarmRepository.setEnabled(alarm.id, false)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                this@RingingForegroundService,
                AlarmConstants.mainRequestCode(alarmId) + 1,
                Intent(this@RingingForegroundService, AlarmRingingActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(EXTRA_ALARM_ID, alarmId)
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
            val stopPendingIntent = PendingIntent.getService(
                this@RingingForegroundService,
                AlarmConstants.mainRequestCode(alarmId) + 2,
                Intent(this@RingingForegroundService, RingingForegroundService::class.java).apply {
                    action = ACTION_STOP_RINGING
                },
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

            val notification = notificationHelper.notifyRinging(alarm, fullScreenPendingIntent, stopPendingIntent)
            startForeground(AlarmConstants.NOTIF_ID_RINGING_BASE + alarm.id.toInt(), notification)

            startSoundAndVibration(alarm.soundUri, alarm.vibrate)
            launchRingingActivity(alarmId)
            startWatchdog()
        }
    }

    private fun launchRingingActivity(alarmId: Long) {
        val intent = Intent(this, AlarmRingingActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        // Se lanza directamente además del fullScreenIntent de la notificación: el fullScreenIntent
        // es lo que garantiza que se muestre sobre la pantalla de bloqueo; este startActivity directo
        // cubre el caso de pantalla desbloqueada, donde el sistema si no mostraría solo un heads-up.
        // En MIUI requiere el permiso "Mostrar ventanas emergentes en segundo plano" (ver XiaomiPermissionHelper).
        runCatching { startActivity(intent) }
    }

    private fun startWatchdog() {
        watchdogRunnable = object : Runnable {
            override fun run() {
                val alarmId = ringingAlarmId ?: return
                if (!AlarmRingingActivity.isVisible) {
                    launchRingingActivity(alarmId)
                }
                watchdogTicks++
                // Primero reintenta rápido (cada 3s, 10 veces), luego cada 12s indefinidamente
                // mientras la alarma siga sonando, para sobrevivir a que el usuario la mande a segundo plano.
                val delay = if (watchdogTicks < 10) 3_000L else 12_000L
                watchdogHandler.postDelayed(this, delay)
            }
        }
        watchdogHandler.postDelayed(watchdogRunnable!!, 3_000L)
    }

    private fun startSoundAndVibration(soundUri: String?, vibrate: Boolean) {
        runCatching {
            val uri = soundUri?.let { Uri.parse(it) }
                ?: RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getValidRingtoneUri(this)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                setDataSource(this@RingingForegroundService, uri!!)
                isLooping = true
                prepare()
                start()
            }
        }

        if (vibrate) {
            val pattern = longArrayOf(0, 800, 500)
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getSystemService<VibratorManager>()?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService<Vibrator>()
            }
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        }
    }

    private fun acquireWakeLock() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeUp:AlarmRingingWakeLock",
        ).apply { acquire(10 * 60 * 1000L) }
    }

    private fun stopRinging() {
        val alarmId = ringingAlarmId
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        runCatching { wakeLock?.release() }
        wakeLock = null
        if (alarmId != null) notificationHelper.cancelRinging(alarmId)
        ringingAlarmId = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RINGING = "com.fritangui.wakeup.action.START_RINGING"
        const val ACTION_STOP_RINGING = AlarmConstants.ACTION_STOP_RINGING

        fun stopIntent(context: Context): Intent =
            Intent(context, RingingForegroundService::class.java).apply { action = ACTION_STOP_RINGING }
    }
}
