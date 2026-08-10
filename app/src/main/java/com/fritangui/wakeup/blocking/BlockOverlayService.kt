package com.fritangui.wakeup.blocking

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
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
import com.fritangui.wakeup.data.db.entity.BlockSurface
import dagger.hilt.android.AndroidEntryPoint
import kotlin.time.Duration.Companion.minutes

/**
 * Overlay a pantalla completa que se muestra cuando el usuario agota su tiempo diario en una
 * superficie bloqueada (p.ej. Reels). Ofrece dos salidas reales en vez de un solo botón de
 * "Entendido" que no cambiaba nada: "Cerrar app" (manda a inicio) o "5 minutos más" (deja seguir
 * usando la app, pero con un velo gris encima mientras dura la prórroga; al acabarse, este mismo
 * bloqueo vuelve a aparecer si sigues ahí).
 */
@AndroidEntryPoint
class BlockOverlayService : LifecycleService() {

    private var overlayView: View? = null
    private val handler = Handler(Looper.getMainLooper())
    private var pendingCallback: Runnable? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        val label = intent?.getStringExtra(EXTRA_LABEL) ?: "esta app"
        val surface = intent?.getStringExtra(EXTRA_SURFACE)?.let { runCatching { BlockSurface.valueOf(it) }.getOrNull() }
        startForeground(NOTIF_ID, buildBlockedNotification())
        showBlockOverlay(label, surface)
        return START_NOT_STICKY
    }

    private fun buildBlockedNotification() =
        NotificationCompat.Builder(this, WakeUpApp.CHANNEL_USAGE_NAG)
            .setSmallIcon(R.drawable.ic_notification_usage)
            .setContentTitle("Límite diario alcanzado")
            .setContentText("Wake up bloqueó el contenido temporalmente")
            .setOngoing(true)
            .build()

    private fun showBlockOverlay(label: String, surface: BlockSurface?) {
        removeCurrentOverlay()
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
            text = "Ya usaste tu tiempo de hoy en $label."
            setTextColor(Color.LTGRAY)
            textSize = 16f
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 48)
        }
        val closeButton = Button(this).apply {
            text = "Cerrar app"
            setOnClickListener {
                ReelsBlockAccessibilityService.requestGoHome()
                removeOverlayAndStop()
            }
        }
        val graceButton = Button(this).apply {
            text = "5 minutos más"
            setOnClickListener {
                if (surface != null) ReelsBlockAccessibilityService.snooze(surface, GRACE_DURATION)
                showGraceOverlay(label)
            }
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
            params.topMargin = 24
            layoutParams = params
        }
        container.addView(title)
        container.addView(message)
        container.addView(closeButton)
        container.addView(graceButton)
        addOverlayView(container, touchable = true)

        // Red de seguridad: si por lo que sea nadie toca ningún botón, no se queda pegado bloqueando
        // la pantalla para siempre — se resuelve solo como si hubiera tocado "Cerrar app".
        pendingCallback = Runnable {
            ReelsBlockAccessibilityService.requestGoHome()
            removeOverlayAndStop()
        }
        handler.postDelayed(pendingCallback!!, AUTO_CLOSE_FALLBACK_MS)
    }

    /**
     * Android no deja que una app normal (sin el permiso de sistema WRITE_SECURE_SETTINGS, que solo
     * se puede otorgar por ADB) prenda la escala de grises real de todo el sistema. Esto se le
     * acerca: un velo gris translúcido encima de todo, que no bloquea el toque (FLAG_NOT_TOUCHABLE,
     * así la app de abajo se sigue pudiendo usar con normalidad) pero le quita viveza a los colores
     * mientras dura la prórroga.
     */
    private fun showGraceOverlay(label: String) {
        removeCurrentOverlay()
        val overlay = View(this).apply { setBackgroundColor(Color.parseColor("#AA3A3A3A")) }
        addOverlayView(overlay, touchable = false)
        startForeground(NOTIF_ID, buildGraceNotification(label))

        pendingCallback = Runnable {
            // Se acabó la prórroga: quita el velo. Si el usuario sigue en la superficie bloqueada,
            // ReelsBlockAccessibilityService.checkLimit la detecta de nuevo en su próximo evento (el
            // snooze ya venció) y este mismo overlay de bloqueo vuelve a aparecer solo.
            removeOverlayAndStop()
        }
        handler.postDelayed(pendingCallback!!, GRACE_DURATION.inWholeMilliseconds)
    }

    private fun buildGraceNotification(label: String) =
        NotificationCompat.Builder(this, WakeUpApp.CHANNEL_USAGE_NAG)
            .setSmallIcon(R.drawable.ic_notification_usage)
            .setContentTitle("5 minutos más en $label")
            .setContentText("Wake up atenuó la pantalla mientras dura la prórroga")
            .setOngoing(true)
            .build()

    private fun addOverlayView(view: View, touchable: Boolean) {
        val windowManager = getSystemService<WindowManager>() ?: return stopSelf()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        }
        // Sin FLAG_NOT_FOCUSABLE en el bloqueo (debe poder recibir el toque de sus botones); con
        // FLAG_NOT_TOUCHABLE + FLAG_NOT_FOCUSABLE en el velo de gracia (debe dejar pasar el toque a
        // la app de abajo).
        val flags = if (touchable) 0 else (WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE or WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE)
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            flags,
            PixelFormat.TRANSLUCENT,
        )
        runCatching { windowManager.addView(view, params) }.onFailure { stopSelf(); return }
        overlayView = view
    }

    private fun removeOverlayAndStop() {
        removeCurrentOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun removeCurrentOverlay() {
        pendingCallback?.let { handler.removeCallbacks(it) }
        pendingCallback = null
        val windowManager = getSystemService<WindowManager>()
        overlayView?.let { runCatching { windowManager?.removeView(it) } }
        overlayView = null
    }

    override fun onDestroy() {
        removeCurrentOverlay()
        super.onDestroy()
    }

    companion object {
        private const val NOTIF_ID = 61_000
        private const val EXTRA_LABEL = "extra_label"
        private const val EXTRA_SURFACE = "extra_surface"
        private const val AUTO_CLOSE_FALLBACK_MS = 45_000L
        private val GRACE_DURATION = 5.minutes

        fun intent(context: Context, label: String, surface: BlockSurface) =
            Intent(context, BlockOverlayService::class.java)
                .putExtra(EXTRA_LABEL, label)
                .putExtra(EXTRA_SURFACE, surface.name)
    }
}
