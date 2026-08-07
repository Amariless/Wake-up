package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.unit.ColorProvider
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.fritangui.wakeup.MainActivity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceDark
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Widget de home screen con las próximas tareas a vencer. */
class NextTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val tasks = entryPoint.taskRepository().observeUpcoming(limit = 6).first()
        val openAppIntent = Intent(context, MainActivity::class.java)

        provideContent {
            WidgetContent(tasks, openAppIntent)
        }
    }

    @Composable
    private fun WidgetContent(items: List<TaskEntity>, openAppIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpSurfaceDark)
                .padding(12.dp)
                .clickable(actionStartActivity(openAppIntent)),
        ) {
            Text(
                "Próximas tareas",
                style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold),
            )
            if (items.isEmpty()) {
                Text("Sin tareas próximas", style = TextStyle(color = ColorProvider(WakeUpOnPrimary)))
            } else {
                items.forEach { task ->
                    Text(
                        task.title + (task.dueAtEpochMillis?.let { " · " + formatDue(it) } ?: ""),
                        style = TextStyle(color = ColorProvider(WakeUpOnPrimary)),
                        modifier = GlanceModifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    private fun formatDue(epochMillis: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        return "%02d/%02d".format(dt.dayOfMonth, dt.monthNumber)
    }
}

class NextTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTasksWidget()
}
