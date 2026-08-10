package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.permissions.PermissionStatus
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import com.fritangui.wakeup.ui.theme.WakeUpOutlineDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerDark
import kotlinx.coroutines.flow.first

private data class TopAppUsage(val packageName: String, val label: String, val minutes: Long)

/** Tercer widget de home screen: resumen del tiempo de pantalla de hoy y las apps que más se usaron. */
class ScreenTimeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val hasAccess = PermissionStatus.hasUsageAccess(context)
        val usage = if (hasAccess) entryPoint.usageRepository().observeForDay(todayEpochDay()).first() else emptyList()
        val totalMinutes = usage.sumOf { it.minutesUsed }
        val topApps = usage.sortedByDescending { it.minutesUsed }.take(3)
            .map { entry -> TopAppUsage(entry.packageName, resolveLabel(context, entry.packageName), entry.minutesUsed) }
        val openIntent = WidgetDeepLink.screenTimeIntent(context)

        provideContent {
            WidgetContent(hasAccess, totalMinutes, topApps, openIntent)
        }
    }

    @Composable
    private fun WidgetContent(hasAccess: Boolean, totalMinutes: Long, topApps: List<TopAppUsage>, openIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpSurfaceContainerDark)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(openIntent))
                .padding(16.dp),
        ) {
            Text(
                "Tiempo de pantalla",
                style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontWeight = FontWeight.Medium, fontSize = 12.sp),
            )
            if (!hasAccess) {
                Text(
                    "Falta el permiso de uso",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 13.sp),
                    modifier = GlanceModifier.padding(top = 8.dp),
                )
                return@Column
            }
            Text(
                formatDuration(totalMinutes),
                style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold, fontSize = 26.sp),
                modifier = GlanceModifier.padding(top = 4.dp),
            )
            Text("hoy", style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 12.sp))
            if (topApps.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(10.dp))
                topApps.forEachIndexed { index, app ->
                    // El primero (el que más se usó) con el círculo más claro; cada uno debajo va
                    // perdiendo contraste, así de un vistazo se nota cuál pesa más sin leer los
                    // números — no solo el orden de la lista.
                    val fade = if (topApps.size > 1) index.toFloat() / (topApps.size - 1) else 0f
                    val badgeColor = lerp(BADGE_BRIGHT, BADGE_DIM, fade)
                    TopAppRow(app, badgeColor)
                }
            }
        }
    }

    @Composable
    private fun TopAppRow(app: TopAppUsage, badgeColor: Color) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                // Un poco del color propio de esta app de fondo (derivado de su nombre de paquete,
                // ya que no tenemos una paleta real por app): le da variedad visual a la lista sin
                // depender de datos que no existen.
                .background(ColorProvider(colorForPackage(app.packageName).copy(alpha = 0.10f)))
                .cornerRadius(10.dp)
                .padding(vertical = 6.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // El tiempo primero (a la izquierda), envuelto en su propio "círculo" (una píldora bien
            // redondeada) en vez del nombre de la app — es el dato que más importa de un vistazo.
            Box(
                modifier = GlanceModifier
                    .background(ColorProvider(badgeColor))
                    .cornerRadius(16.dp)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
            ) {
                Text(
                    formatDuration(app.minutes),
                    style = TextStyle(color = ColorProvider(Color(0xFF2B2B2B)), fontWeight = FontWeight.Bold, fontSize = 14.sp),
                )
            }
            Spacer(modifier = GlanceModifier.width(10.dp))
            Text(
                app.label,
                style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontSize = 15.sp),
                maxLines = 1,
            )
        }
    }

    private fun resolveLabel(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** Color determinístico a partir del nombre de paquete (no tenemos una paleta real por app). */
    private fun colorForPackage(packageName: String): Color {
        val hue = ((packageName.hashCode() % 360) + 360) % 360
        return Color.hsv(hue.toFloat(), 0.55f, 0.65f)
    }

    /** Igual que en ScreenTimeScreen.kt: "45m" bajo una hora, "1h 23m" (o "2h") de ahí para arriba. */
    private fun formatDuration(minutes: Long): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    companion object {
        private val BADGE_BRIGHT = Color(0xFFF4EAD5) // blanco/beige, la fila con más uso
        private val BADGE_DIM = Color(0xFF7A7466) // pierde contraste según baja en la lista
    }
}

class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
}
