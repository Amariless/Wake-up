package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.fritangui.wakeup.MainActivity
import com.fritangui.wakeup.data.db.entity.TaskEntity
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import com.fritangui.wakeup.ui.theme.WakeUpOutlineDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerHighDark
import kotlinx.coroutines.flow.first
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

/** Widget de home screen con las próximas tareas a vencer, agrupadas por fecha. */
class NextTasksWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val tasks = entryPoint.taskRepository().observeUpcoming(limit = 12).first()
        val subjectColors = entryPoint.subjectRepository().observeWithSessionsForActiveFolders().first()
            .associate { it.subject.id to it.subject.colorArgb }
        val openAppIntent = Intent(context, MainActivity::class.java)

        provideContent {
            WidgetContent(tasks, subjectColors, openAppIntent)
        }
    }

    @Composable
    private fun WidgetContent(items: List<TaskEntity>, subjectColors: Map<Long, Int>, openAppIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpSurfaceContainerDark)
                .cornerRadius(20.dp)
                .clickable(actionStartActivity(openAppIntent)),
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = GlanceModifier.size(8.dp).background(ColorProvider(WakeUpSecondary)).cornerRadius(4.dp)) {}
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    "Próximas tareas",
                    style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                )
            }

            if (items.isEmpty()) {
                Text(
                    "Sin tareas próximas",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark)),
                    modifier = GlanceModifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    var lastGroup: String? = null
                    items.forEach { task ->
                        val group = dueGroupLabel(task.dueAtEpochMillis)
                        if (group != lastGroup) {
                            lastGroup = group
                            item { DayHeader(group) }
                        }
                        item { TaskRow(task, subjectColors[task.subjectId]) }
                    }
                }
            }
        }
    }

    @Composable
    private fun DayHeader(label: String) {
        Text(
            label,
            style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontWeight = FontWeight.Medium, fontSize = 12.sp),
            modifier = GlanceModifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 2.dp),
        )
    }

    @Composable
    private fun TaskRow(task: TaskEntity, subjectColorArgb: Int?) {
        Row(
            modifier = GlanceModifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(28.dp)
                    .background(ColorProvider(subjectColorArgb?.let { Color(it) } ?: WakeUpOutlineDark))
                    .cornerRadius(2.dp),
            ) {}
            Spacer(modifier = GlanceModifier.width(10.dp))
            Column(
                modifier = GlanceModifier
                    .background(WakeUpSurfaceContainerHighDark)
                    .cornerRadius(12.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(
                    task.title,
                    style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                )
                if (task.dueAtEpochMillis != null) {
                    Text(formatDue(task.dueAtEpochMillis), style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 12.sp))
                }
            }
        }
    }

    private fun dueGroupLabel(epochMillis: Long?): String {
        if (epochMillis == null) return "Sin fecha"
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
        val dueDate = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault()).date
        return when (dueDate) {
            today -> "Hoy"
            LocalDate.fromEpochDays(today.toEpochDays() + 1) -> "Mañana"
            else -> "%02d/%02d".format(dueDate.dayOfMonth, dueDate.monthNumber)
        }
    }

    private fun formatDue(epochMillis: Long): String {
        val dt = Instant.fromEpochMilliseconds(epochMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        return "Vence %02d/%02d %02d:%02d".format(dt.dayOfMonth, dt.monthNumber, dt.hour, dt.minute)
    }

    companion object {
        private val WakeUpSecondary = com.fritangui.wakeup.ui.theme.WakeUpSecondary
    }
}

class NextTasksWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextTasksWidget()
}
