package com.fritangui.wakeup.blocking

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import androidx.lifecycle.LifecycleService
import com.fritangui.wakeup.R
import com.fritangui.wakeup.WakeUpApp
import dagger.hilt.android.AndroidEntryPoint

/**
 * Overlay a pantalla completa que se muestra cuando el usuario agota su
 * tiempo diario en una superficie bloqueada (p.ej. Reels). Es intencionalmente
 * de corta duración (se auto-cierra) — lo que realmente saca al usuario de
 * Reels es la acción "atrás" que dispara [ReelsBlockAccessibilityService];
 * este overlay es el aviso visual de por qué pasó.
 */
@AndroidEntryPoint
class BlockOverlayService : LifecycleService() {

    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var autoDismiss: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "esta app"
        startForeground(NOTIF_ID, buildNotification())
        showOverlay(label)
        return START_NOT_STICKY
    }

    private fun buildNotification() =
        NotificationCompat.Builder(this, WakeUpApp.CHANNEL_USAGE_NAG)
            .setSmallIcon(R.drawable.ic_notification_usage)
            .setContentTitle("Límite diario alcanzado")
            .setContentText("Wake up bloqueó el contenido temporalmente")
            .setOngoing(true)
            .build()

    private fun showOverlay(label: String) {
        val windowManager = getSystemService<WindowManager>() ?: return stopSelf()
        if (overlayView != null) return

        val container = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.parseColor("#E6101319"))
            setPadding(64, 64, 64, 64)
        }
        val title = TextView(this).apply {
            text = "⏰ Límite diario alcanzado"
            setTextColor(Color.WHITE)
            textSize = 24f
            typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER
        }
        val message = TextView(this).apply {
            text = "Ya usaste tu tiempo de hoy en $label. Puedes ajustar el límite desde Wake up."
            setTextColor(Color.LTGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 32)
        }
        val dismissButton = Button(this).apply {
            text = "Entendido"
            setOnClickListener { removeOverlayAndStop() }
        }
        container.addView(title)
        container.addView(message)
        container.addView(dismissButton)
        overlayView = container

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            0, // sin FLAG_NOT_FOCUSABLE: debe poder recibir el toque del botón "Entendido"
            android.graphics.PixelFormat.TRANSLUCENT,
        )
        runCatching { windowManager.addView(container, params) }.onFailure { stopSelf() }

        autoDismiss = Runnable { removeOverlayAndStop() }
        handler.postDelayed(autoDismiss!!, AUTO_DISMISS_MS)
    }

    private fun removeOverlayAndStop() {
        autoDismiss?.let { handler.removeCallbacks(it) }
        val windowManager = getSystemService<WindowManager>()
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        overlayView?.let { view -> runCatching { getSystemService<WindowManager>()?.removeView(view) } }
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 61_000
        private const val AUTO_DISMISS_MS = 6_000L
        private const val EXTRA_LABEL = "extra_label"

        fun intent(context: Context, label: String) =
            Intent(context, BlockOverlayService::class.java).putExtra(EXTRA_LABEL, label)
    }
}
