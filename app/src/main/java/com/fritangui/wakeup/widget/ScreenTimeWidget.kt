package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
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
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
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

/** Tercer widget de home screen: resumen del tiempo de pantalla de hoy y las apps que más se usaron. */
class ScreenTimeWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val hasAccess = PermissionStatus.hasUsageAccess(context)
        val usage = if (hasAccess) entryPoint.usageRepository().observeForDay(todayEpochDay()).first() else emptyList()
        val totalMinutes = usage.sumOf { it.minutesUsed }
        val topApps = usage.sortedByDescending { it.minutesUsed }.take(3)
            .map { entry -> resolveLabel(context, entry.packageName) to entry.minutesUsed }
        val openIntent = WidgetDeepLink.screenTimeIntent(context)

        provideContent {
            WidgetContent(hasAccess, totalMinutes, topApps, openIntent)
        }
    }

    @Composable
    private fun WidgetContent(hasAccess: Boolean, totalMinutes: Long, topApps: List<Pair<String, Long>>, openIntent: Intent) {
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
                topApps.forEach { (label, minutes) ->
                    Text(
                        "$label · ${formatDuration(minutes)}",
                        style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontSize = 12.sp),
                        maxLines = 1,
                        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 2.dp),
                    )
                }
            }
        }
    }

    private fun resolveLabel(context: Context, packageName: String): String = runCatching {
        val pm = context.packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)

    /** Igual que en ScreenTimeScreen.kt: "45m" bajo una hora, "1h 23m" (o "2h") de ahí para arriba. */
    private fun formatDuration(minutes: Long): String {
        if (minutes < 60) return "${minutes}m"
        val h = minutes / 60
        val m = minutes % 60
        return if (m == 0L) "${h}h" else "${h}h ${m}m"
    }
}

class ScreenTimeWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScreenTimeWidget()
}
