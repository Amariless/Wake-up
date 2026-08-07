@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.fritangui.wakeup.ui.clock.alarms.AlarmsListScreen
import com.fritangui.wakeup.ui.clock.stopwatch.StopwatchScreen
import com.fritangui.wakeup.ui.clock.timer.TimerScreen

private val TABS = listOf("Alarmas", "Temporizador", "Cronómetro")

@Composable
fun ClockScreen(onOpenAlarm: (Long) -> Unit, onNewAlarm: () -> Unit) {
    var tabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Reloj") }) },
        floatingActionButton = {
            if (tabIndex == 0) {
                FloatingActionButton(onClick = onNewAlarm) { Icon(Icons.Default.Add, contentDescription = "Nueva alarma") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                TABS.forEachIndexed { index, title ->
                    Tab(selected = tabIndex == index, onClick = { tabIndex = index }, text = { Text(title) })
                }
            }
            when (tabIndex) {
                0 -> AlarmsListScreen(onOpenAlarm = onOpenAlarm)
                1 -> TimerScreen()
                else -> StopwatchScreen()
            }
        }
    }
}
