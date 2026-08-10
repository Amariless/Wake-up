package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
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
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fritangui.wakeup.domain.todayEpochDay
import com.fritangui.wakeup.permissions.PermissionStatus
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import com.fritangui.wakeup.ui.theme.WakeUpOutlineDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerDark
import kotlinx.coroutines.flow.first

private data class TopAppUsage(val packageName: String, val label: String, val minutes: Long)

/**
 * Tercer widget de home screen: resumen del tiempo de pantalla de hoy y las apps que más se
 * usaron. Formato pedido por el usuario (captura de referencia): título + botón de refrescar
 * arriba, el total grande debajo, y cada app en su propia fila con el tiempo dentro de un círculo
 * perfecto a la izquierda (no una píldora) y el nombre a la derecha.
 */
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
                .padding(16.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tiempo de uso",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openIntent)),
                )
                // Refresca los datos del widget sin tener que abrir la app — provideGlance se vuelve
                // a llamar con la consulta más reciente a Room/UsageStatsManager.
                Text(
                    "⟳",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 16.sp),
                    modifier = GlanceModifier.clickable(actionRunCallback<RefreshScreenTimeAction>()),
                )
            }
            if (!hasAccess) {
                Text(
                    "Falta el permiso de uso",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 13.sp),
                    modifier = GlanceModifier.padding(top = 8.dp).clickable(actionStartActivity(openIntent)),
                )
                return@Column
            }
            Text(
                formatDuration(totalMinutes),
                style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold, fontSize = 30.sp),
                modifier = GlanceModifier.padding(top = 4.dp).clickable(actionStartActivity(openIntent)),
            )
            if (topApps.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(14.dp))
                topApps.forEach { app -> TopAppRow(app, openIntent) }
            }
        }
    }

    @Composable
    private fun TopAppRow(app: TopAppUsage, openIntent: Intent) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = 6.dp).clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Círculo perfecto (mismo ancho y alto + esquinas a la mitad de ese tamaño) en vez de
            // una píldora de ancho variable, igual que en la referencia — el color es propio de
            // cada app (derivado de su paquete, ya que no hay una paleta real por app disponible).
            Box(
                modifier = GlanceModifier
                    .size(CIRCLE_SIZE)
                    .background(ColorProvider(colorForPackage(app.packageName)))
                    .cornerRadius(CIRCLE_SIZE / 2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatDuration(app.minutes).replace(" ", ""),
                    style = TextStyle(
                        color = ColorProvider(Color(0xFF232323)),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
            }
            Spacer(modifier = GlanceModifier.width(12.dp))
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

    /** Color determinístico y pastel (bastante claro, para que el texto oscuro adentro se lea bien) a partir del paquete. */
    private fun colorForPackage(packageName: String): Color {
        val hue = ((packageName.hashCode() % 360) + 360) % 360
        return Color.hsv(hue.toFloat(), 0.35f, 0.92f)
    }

    /** Igual que en ScreenTimeScreen.kt: "45m" bajo una hora, "1h 23m" (o "2h") de ahí para arriba. */
    private fun formatDuration(minutes: Long): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    companion object {
        private val CIRCLE_SIZE = 40.dp
    }
}

/** Botón ⟳ del encabezado: vuelve a llamar provideGlance con los datos más recientes. */
class RefreshScreenTimeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        ScreenTimeWidget().update(context, glanceId)
    }
}

class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
}
