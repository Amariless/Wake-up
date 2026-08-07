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
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.domain.computeNextClassOccurrences
import com.fritangui.wakeup.ui.theme.WakeUpBackgroundDark
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import kotlinx.coroutines.flow.first

/**
 * Widget de home screen con las próximas clases (día, hora y salón), al estilo
 * del widget de lista de Google Calendar.
 */
class NextClassesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val subjects = entryPoint.subjectRepository().observeWithSessionsForActiveFolders().first()
        val occurrences = computeNextClassOccurrences(subjects, limit = 6)
        val openAppIntent = Intent(context, MainActivity::class.java)

        provideContent {
            WidgetContent(occurrences, openAppIntent)
        }
    }

    @Composable
    private fun WidgetContent(items: List<UpcomingClassOccurrence>, openAppIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpBackgroundDark)
                .padding(12.dp)
                .clickable(actionStartActivity(openAppIntent)),
        ) {
            Text(
                "Próximas clases",
                style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold),
            )
            if (items.isEmpty()) {
                Text(
                    "Sin clases próximas",
                    style = TextStyle(color = ColorProvider(WakeUpOnPrimary)),
                )
            } else {
                items.forEach { occurrence ->
                    Text(
                        "%s %02d:%02d · %s · %s".format(
                            DIA_CORTO[occurrence.start.dayOfWeek.value - 1],
                            occurrence.start.hour,
                            occurrence.start.minute,
                            occurrence.subjectName,
                            occurrence.room,
                        ),
                        style = TextStyle(color = ColorProvider(WakeUpOnPrimary)),
                        modifier = GlanceModifier.padding(top = 4.dp),
                    )
                }
            }
        }
    }

    companion object {
        private val DIA_CORTO = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")
    }
}

class NextClassesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassesWidget()
}
