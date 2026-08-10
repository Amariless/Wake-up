package com.fritangui.wakeup

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.fritangui.wakeup.data.db.AppDatabase
import com.fritangui.wakeup.data.repository.FolderRepository
import com.fritangui.wakeup.usage.ScreenTimeWorker
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application raíz. Habilita Hilt para inyección de dependencias en toda la app
 * (activities, services, workers, receivers) y registra los canales de notificación
 * necesarios en el arranque, ya que en Android 8+ un canal debe existir *antes* de
 * poder postear cualquier notificación en él. También configura WorkManager con
 * el factory de Hilt (así los Workers pueden recibir dependencias inyectadas) y
 * programa el trabajo periódico de análisis de uso de pantalla.
 */
@HiltAndroidApp
class WakeUpApp : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var database: AppDatabase
    @Inject lateinit var folderRepository: FolderRepository

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        ScreenTimeWorker.enqueuePeriodic(this)
        warmUpDatabase()
        cleanUpStrayDemoData()
    }

    /**
     * Limpieza de una sola vez: el botón "Sembrar datos de ejemplo" del panel de desarrollador
     * (solo en builds debug) crea una carpeta real "Demo 2026-1" con su materia "Cálculo I" — si
     * alguna vez se tocó sin querer, esa carpeta terminaba mezclada con datos reales del usuario
     * (visible en "próximas clases" de Inicio aunque no siempre obvia en la lista de Carpetas si
     * hay una carpeta marcada como principal). Se borra sola en cuanto la app arranca; si no
     * existe, esto no hace nada.
     */
    private fun cleanUpStrayDemoData() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            folderRepository.deleteByExactName("Demo 2026-1")
        }
    }

    /**
     * Abre la conexión SQLite (WAL, etc.) en un hilo de fondo apenas arranca la app, en vez de
     * dejar que esto lo pague la primera pantalla que el usuario visite: sin esto, la primera
     * consulta de Room de toda la sesión (la que dispara la primera pestaña que se abre) tiene
     * que esperar a que se abra el archivo de la BD, y eso se sentía como que "la primera vez
     * que abres una pestaña" iba más lento que las siguientes.
     */
    private fun warmUpDatabase() {
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            database.openHelper.writableDatabase
        }
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return

        val alarmChannel = NotificationChannel(
            CHANNEL_ALARM,
            "Alarmas sonando",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notificación persistente mientras una alarma está sonando"
            setSound(null, null) // el sonido lo maneja el propio servicio de la alarma
            enableVibration(false)
            setBypassDnd(true)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val preAlarmChannel = NotificationChannel(
            CHANNEL_PRE_ALARM,
            "Aviso previo de alarma",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Aviso una hora antes de que suene una alarma (silencioso, sin sonido ni vibración)"
            setSound(null, null)
            enableVibration(false)
        }

        val remindersChannel = NotificationChannel(
            CHANNEL_TASK_REMINDERS,
            "Recordatorios de tareas",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Recordatorios de vencimiento de tareas académicas"
        }

        val usageChannel = NotificationChannel(
            CHANNEL_USAGE_NAG,
            "Tiempo de pantalla",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Avisos de tiempo de uso en redes sociales y límites de bloqueo"
        }

        val timerRunningChannel = NotificationChannel(
            CHANNEL_TIMER_RUNNING,
            "Temporizador en curso",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificación silenciosa con el tiempo restante del temporizador"
        }

        manager.createNotificationChannels(
            listOf(alarmChannel, preAlarmChannel, remindersChannel, usageChannel, timerRunningChannel)
        )
    }

    companion object {
        const val CHANNEL_ALARM = "channel_alarm_ringing"
        const val CHANNEL_PRE_ALARM = "channel_pre_alarm"
        const val CHANNEL_TASK_REMINDERS = "channel_task_reminders"
        const val CHANNEL_USAGE_NAG = "channel_usage_nag"
        const val CHANNEL_TIMER_RUNNING = "channel_timer_running"
    }
}
