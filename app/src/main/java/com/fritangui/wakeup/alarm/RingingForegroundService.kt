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
import com.fritangui.wakeup.alarm.sound.AlarmSounds
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
    /** Si llega otra alarma mientras esta suena, se guarda acá en vez de perderse: arranca sola al apagar la actual. */
    private var queuedAlarmId: Long? = null

    private var vibrateEnabled = false
    private var muteRunnable: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_START_RINGING -> {
                val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
                if (alarmId >= 0) {
                    if (ringingAlarmId == null) startRinging(alarmId) else queuedAlarmId = alarmId
                }
            }
            ACTION_STOP_RINGING -> {
                val next = queuedAlarmId
                queuedAlarmId = null
                stopRinging(hasNext = next != null)
                if (next != null) startRinging(next)
            }
            ACTION_MUTE_TEMPORARILY -> muteTemporarily()
        }
        return START_STICKY
    }

    /**
     * Le baja mucho el volumen a la alarma (no la deja en silencio total) para dar un respiro y
     * poder concentrarse en el reto sin el ruido encima. Si en ese minuto no se apagó de verdad
     * (completando el reto), vuelve sola a su volumen normal — a propósito: dejar que una alarma se
     * pueda callar del todo indefinidamente derrota el propósito de que exista.
     */
    private fun muteTemporarily() {
        runCatching { mediaPlayer?.setVolume(QUIET_VOLUME, QUIET_VOLUME) }
        vibrator?.cancel()
        muteRunnable?.let { watchdogHandler.removeCallbacks(it) }
        muteRunnable = Runnable {
            if (ringingAlarmId != null) {
                runCatching { mediaPlayer?.setVolume(1f, 1f) }
                if (vibrateEnabled) startVibration()
            }
        }
        watchdogHandler.postDelayed(muteRunnable!!, MUTE_DURATION_MS)
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
            // Si el usuario pidió "eliminar después de sonar", en cambio, no hay próxima ocurrencia
            // que armar: se cancela cualquier programación futura y se borra la fila de una vez
            // (sigue sonando esta vez con normalidad; lo que cambia es que no queda guardada).
            if (alarm.deleteAfterRing) {
                alarmScheduler.cancelAlarm(alarm.id)
                alarmRepository.delete(alarm)
            } else {
                // Antes, sin días marcados (bitmask 0) se apagaba sola tras sonar una vez — eso era
                // cuando esa combinación significaba "una sola vez". Ahora bitmask 0 sin
                // deleteAfterRing significa "todos los días" (ver AlarmTiming.nextTrigger), así que
                // se reprograma igual que cualquier alarma repetitiva, sin desactivarse.
                alarmScheduler.scheduleAlarm(alarm)
                alarmRepository.setLastTriggered(alarm.id)
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
            // El aviso T-60 ("suena a las...") ya cumplió su propósito en cuanto la alarma real
            // suena — antes se quedaba pegado en la barra de notificaciones hasta que el usuario lo
            // descartara a mano (#138).
            notificationHelper.cancelPreAlarmNotification(alarm.id)

            val notification = notificationHelper.notifyRinging(alarm, fullScreenPendingIntent)
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
                // Renueva el wakelock en cada tick del watchdog (cada 3-12s) en vez de un único
                // acquire con timeout fijo: una alarma que suena más de ese timeout sin que el
                // usuario interactúe (p.ej. el teléfono queda en un bolsillo) podía perder el
                // PARTIAL_WAKE_LOCK a mitad de sonar.
                renewWakeLock()
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
        val primaryUri = soundUri?.let { Uri.parse(it) } ?: Uri.parse(AlarmSounds.defaultSoundUriFor(this))
        val started = runCatching { playAlarmSound(primaryUri) }.isSuccess
        if (!started) {
            // Si el sonido guardado ya no existe (p.ej. un content:// revocado), cae al del sistema;
            // y si NI SIQUIERA el sistema tiene un tono de alarma configurado (ROM/emulador sin
            // ninguno), cae al sonido embebido de la app, que siempre existe — así nunca se llega al
            // caso de quedar sin ningún sonido en absoluto.
            val systemFallback = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getValidRingtoneUri(this)
            val fallbackUri = systemFallback ?: Uri.parse(AlarmSounds.defaultSoundUriFor(this))
            runCatching { playAlarmSound(fallbackUri) }
        }

        vibrateEnabled = vibrate
        if (vibrate) startVibration()
    }

    private fun playAlarmSound(uri: Uri) {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            setDataSource(this@RingingForegroundService, uri)
            isLooping = true
            prepare()
            start()
        }
    }

    private fun startVibration() {
        val pattern = longArrayOf(0, 800, 500)
        vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            getSystemService<VibratorManager>()?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService<Vibrator>()
        }
        vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
    }

    private fun acquireWakeLock() {
        val pm = getSystemService<PowerManager>() ?: return
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "WakeUp:AlarmRingingWakeLock",
        ).apply {
            // Sin referencia contada: cada renewWakeLock() de abajo solo necesita reemplazar el
            // timeout, no acumular acquires que exigirían la misma cantidad de release() para
            // soltarse de verdad.
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    /** Extiende el timeout del wakelock ya adquirido; ver comentario en [startWatchdog]. */
    private fun renewWakeLock() {
        wakeLock?.acquire(WAKE_LOCK_TIMEOUT_MS)
    }

    /** @param hasNext si ya hay otra alarma en cola para arrancar justo después, no tira abajo el foreground service. */
    private fun stopRinging(hasNext: Boolean = false) {
        val alarmId = ringingAlarmId
        watchdogRunnable?.let { watchdogHandler.removeCallbacks(it) }
        watchdogRunnable = null
        muteRunnable?.let { watchdogHandler.removeCallbacks(it) }
        muteRunnable = null
        runCatching { mediaPlayer?.stop() }
        runCatching { mediaPlayer?.release() }
        mediaPlayer = null
        vibrator?.cancel()
        vibrator = null
        runCatching { wakeLock?.release() }
        wakeLock = null
        if (alarmId != null) notificationHelper.cancelRinging(alarmId)
        ringingAlarmId = null
        if (!hasNext) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    override fun onDestroy() {
        stopRinging()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RINGING = "com.fritangui.wakeup.action.START_RINGING"
        const val ACTION_STOP_RINGING = AlarmConstants.ACTION_STOP_RINGING
        const val ACTION_MUTE_TEMPORARILY = "com.fritangui.wakeup.action.MUTE_TEMPORARILY"
        // 1 minuto (antes 20s) y ya no baja a 0: una alarma que se puede dejar en silencio total
        // y quieta es una alarma que no cumple su trabajo si te volvés a dormir durante esos
        // segundos. Bajarle mucho el volumen sigue dando el respiro para resolver el reto, pero no
        // deja de sonar del todo — y si en un minuto no se apagó de verdad, vuelve a su volumen normal sola.
        const val MUTE_DURATION_MS = 60_000L
        const val QUIET_VOLUME = 0.15f
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L

        fun stopIntent(context: Context): Intent =
            Intent(context, RingingForegroundService::class.java).apply { action = ACTION_STOP_RINGING }

        fun muteIntent(context: Context): Intent =
            Intent(context, RingingForegroundService::class.java).apply { action = ACTION_MUTE_TEMPORARILY }
    }
}
