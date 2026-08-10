package com.fritangui.wakeup.widget

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
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
import com.fritangui.wakeup.domain.UpcomingClassOccurrence
import com.fritangui.wakeup.domain.computeNextClassOccurrences
import com.fritangui.wakeup.ui.components.amPmSuffix
import com.fritangui.wakeup.ui.components.formatClockTime
import com.fritangui.wakeup.ui.theme.WakeUpOnPrimary
import com.fritangui.wakeup.ui.theme.WakeUpOutlineDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerDark
import com.fritangui.wakeup.ui.theme.WakeUpSurfaceContainerHighDark
import kotlinx.coroutines.flow.first

/**
 * Widget de home screen con las próximas clases (día, hora y salón), al estilo
 * del widget de lista de Google Calendar: agrupadas por día, con scroll si no
 * caben todas, y un color propio por materia.
 */
class NextClassesWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entryPoint = widgetEntryPoint(context)
        val subjects = entryPoint.subjectRepository().observeWithSessionsForActiveFolders().first()
        val occurrences = computeNextClassOccurrences(subjects, limit = 12)
        val use24Hour = entryPoint.settingsDataStore().use24HourFormat.first()
        val openAppIntent = Intent(context, MainActivity::class.java)

        provideContent {
            WidgetContent(occurrences, use24Hour, openAppIntent)
        }
    }

    @Composable
    private fun WidgetContent(items: List<UpcomingClassOccurrence>, use24Hour: Boolean, openAppIntent: Intent) {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(WakeUpSurfaceContainerDark)
                .cornerRadius(20.dp),
        ) {
            Row(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .clickable(actionStartActivity(openAppIntent)),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = GlanceModifier.size(8.dp).background(ColorProvider(WakeUpAccent)).cornerRadius(4.dp)) {}
                Spacer(modifier = GlanceModifier.width(8.dp))
                Text(
                    "Próximas clases",
                    style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Bold, fontSize = 15.sp),
                )
            }

            if (items.isEmpty()) {
                Text(
                    "Sin clases próximas",
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark)),
                    modifier = GlanceModifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp).clickable(actionStartActivity(openAppIntent)),
                )
            } else {
                LazyColumn(modifier = GlanceModifier.fillMaxWidth()) {
                    var lastDay: Int? = null
                    items.forEach { occurrence ->
                        val day = occurrence.start.dayOfWeek.value
                        if (day != lastDay) {
                            lastDay = day
                            item { DayHeader(DIA_LARGO[day - 1]) }
                        }
                        item { ClassRow(occurrence, use24Hour) }
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
    private fun ClassRow(occurrence: UpcomingClassOccurrence, use24Hour: Boolean) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .clickable(
                    actionStartActivity(
                        WidgetDeepLink.subjectIntent(context, occurrence.folderId, occurrence.subjectId),
                    ),
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = GlanceModifier
                    .width(4.dp)
                    .height(32.dp)
                    .background(ColorProvider(Color(occurrence.colorArgb)))
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
                    occurrence.subjectName,
                    style = TextStyle(color = ColorProvider(WakeUpOnPrimary), fontWeight = FontWeight.Medium, fontSize = 13.sp),
                )
                Text(
                    "%s%s–%s%s · %s".format(
                        formatClockTime(occurrence.start.hour, occurrence.start.minute, use24Hour),
                        amPmSuffix(occurrence.start.hour, use24Hour)?.let { " $it" } ?: "",
                        formatClockTime(occurrence.end.hour, occurrence.end.minute, use24Hour),
                        amPmSuffix(occurrence.end.hour, use24Hour)?.let { " $it" } ?: "",
                        occurrence.room,
                    ),
                    style = TextStyle(color = ColorProvider(WakeUpOutlineDark), fontSize = 12.sp),
                )
            }
        }
    }

    companion object {
        private val DIA_LARGO = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
        private val WakeUpAccent = com.fritangui.wakeup.ui.theme.WakeUpPrimaryDark
    }
}

class NextClassesWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = NextClassesWidget()
}
