package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
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
 * a la izquierda y el nombre a la derecha — con tamaño y contraste del círculo según qué tan usada
 * fue esa app hoy (#153).
 */
class ScreenTimeWidget : GlanceAppWidget() {

    // Exact (no Single) para que LocalSize.current adentro del composable refleje el tamaño real
    // que el usuario le dio al widget en su pantalla de inicio — así se puede mostrar una 4ta/5ta
    // app cuando hay alto de sobra, en vez de un número fijo sin importar la resolución (#153).
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val hasAccess = PermissionStatus.hasUsageAccess(context)
        val usage = if (hasAccess) entryPoint.usageRepository().observeForDay(todayEpochDay()).first() else emptyList()
        val totalMinutes = usage.sumOf { it.minutesUsed }
        // Se trae hasta 5 (el máximo que se podría llegar a mostrar); cuántas de esas se pintan de
        // verdad se decide adentro del composable según el alto real del widget.
        val topApps = usage.sortedByDescending { it.minutesUsed }.take(5)
            .map { entry -> TopAppUsage(entry.packageName, resolveLabel(context, entry.packageName), entry.minutesUsed) }
        val openIntent = WidgetDeepLink.screenTimeIntent(context)

        provideContent {
            WidgetContent(hasAccess, totalMinutes, topApps, openIntent)
        }
    }

    @Composable
    private fun WidgetContent(hasAccess: Boolean, totalMinutes: Long, topApps: List<TopAppUsage>, openIntent: Intent) {
        // Antes se usaban umbrales fijos (p.ej. "260dp o más → 5 apps") que no tenían en cuenta el
        // alto REAL que ocupa cada fila — a ese alto ya calculado le sobraban filas y la última
        // quedaba aplastada/cortada (Glance no puede hacer scroll acá). Ahora se calcula cuántas
        // filas caben de verdad restando el resto del contenido (encabezado, total, espaciador,
        // padding) del alto real del widget, en vez de adivinar.
        val heightDp = LocalSize.current.height.value
        val fixedOverheadDp = 28f + 20f + 38f + 10f // padding(14+14) + encabezado + total + espaciador
        val rowHeightDp = CIRCLE_MAX.value + ROW_VERTICAL_PADDING.value * 2
        val maxRowsThatFit = ((heightDp - fixedOverheadDp) / rowHeightDp).toInt().coerceIn(0, 5)
        val visibleApps = topApps.take(maxRowsThatFit)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpSurfaceContainerDark)
                .cornerRadius(20.dp)
                .padding(14.dp),
        ) {
            Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Tiempo de uso",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontWeight = FontWeight.Medium, fontSize = 12.sp),
                    modifier = GlanceModifier.defaultWeight().clickable(actionStartActivity(openIntent)),
                )
                // Refresca los datos del widget sin tener que abrir la app — provideGlance se vuelve
                // a llamar con la consulta más reciente a Room/UsageStatsManager. Ícono más grande
                // que antes (#153): 16sp era casi imposible de tocar con precisión.
                Text(
                    "⟳",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 22.sp),
                    modifier = GlanceModifier.padding(4.dp).clickable(actionRunCallback<RefreshScreenTimeAction>()),
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
            if (visibleApps.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(10.dp))
                val maxMinutes = visibleApps.maxOf { it.minutes }.coerceAtLeast(1L)
                visibleApps.forEachIndexed { index, app ->
                    val fraction = (app.minutes.toFloat() / maxMinutes).coerceIn(0f, 1f)
                    TopAppRow(app, fraction, openIntent)
                }
            }
        }
    }

    @Composable
    private fun TopAppRow(app: TopAppUsage, usageFraction: Float, openIntent: Intent) {
        // Círculo de tamaño dinámico (más grande cuanto más se usó hoy esa app, entre CIRCLE_MIN y
        // CIRCLE_MAX) y con menos aire vertical entre filas que antes, para que las filas de mayor a
        // menor uso se vean "encimadas" en vez de sueltas y parejas (#153).
        val circleSize = CIRCLE_MIN + (CIRCLE_MAX - CIRCLE_MIN) * usageFraction
        val circleColor = colorForRank(app.packageName, usageFraction)
        val textOnCircleColor = if (isLight(circleColor)) Color(0xFF232323) else Color(0xFFF5F5F5)

        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(vertical = ROW_VERTICAL_PADDING).clickable(actionStartActivity(openIntent)),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .size(circleSize)
                    .background(ColorProvider(circleColor))
                    .cornerRadius(circleSize / 2),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    formatDuration(app.minutes).replace(" ", ""),
                    style = TextStyle(
                        color = ColorProvider(textOnCircleColor),
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

    /**
     * Color determinístico (hue propio del paquete, para variedad entre apps) cuyo contraste contra
     * el fondo oscuro del widget va de MÁS vivo (la app más usada) a MÁS apagado/gris (la menos
     * usada de las mostradas) — antes todas las filas tenían el mismo brillo/saturación sin importar
     * qué tan usada fue cada una (#153).
     */
    private fun colorForRank(packageName: String, usageFraction: Float): Color {
        val hue = ((packageName.hashCode() % 360) + 360) % 360
        val saturation = 0.18f + 0.30f * usageFraction
        val value = 0.55f + 0.37f * usageFraction
        return Color.hsv(hue.toFloat(), saturation, value)
    }

    private fun isLight(color: Color): Boolean =
        (0.299f * color.red + 0.587f * color.green + 0.114f * color.blue) > 0.6f

    /** Igual que en ScreenTimeScreen.kt: "45m" bajo una hora, "1h 23m" (o "2h") de ahí para arriba. */
    private fun formatDuration(minutes: Long): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }

    companion object {
        private val CIRCLE_MIN = 26.dp
        private val CIRCLE_MAX = 40.dp
        private val ROW_VERTICAL_PADDING = 2.dp
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
