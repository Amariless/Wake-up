@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.domain.AlarmTiming
import com.fritangui.wakeup.ui.clock.alarms.AlarmsListScreen
import com.fritangui.wakeup.ui.clock.alarms.AlarmsViewModel
import com.fritangui.wakeup.ui.clock.stopwatch.StopwatchScreen
import com.fritangui.wakeup.ui.clock.timer.TimerScreen
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

private val TABS = listOf("Alarmas", "Temporizador", "Cronómetro")

@Composable
fun ClockScreen(onOpenAlarm: (Long) -> Unit, onNewAlarm: () -> Unit) {
    // #145: antes solo se podía cambiar de sub-pestaña tocándola; con HorizontalPager también se
    // puede deslizar hacia el lado. pagerState ya persiste la página seleccionada (rememberSaveable
    // por dentro), así que reemplaza al `tabIndex` suelto de antes.
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val coroutineScope = rememberCoroutineScope()
    val tabIndex = pagerState.currentPage
    // La misma instancia que usa AlarmsListScreen (comparten NavBackStackEntry): se pasa
    // explícitamente para no depender de que Hilt la resuelva igual en los dos sitios.
    val alarmsViewModel: AlarmsViewModel = hiltViewModel()
    val alarms by alarmsViewModel.alarms.collectAsState()

    var now by remember { mutableStateOf(Clock.System.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(30_000)
            now = Clock.System.now()
        }
    }
    // #139: además del "Faltan X" de cada fila, un resumen a nivel de pantalla de cuándo suena la
    // PRÓXIMA alarma en general (la más próxima entre todas las habilitadas), visible sin importar
    // en qué sub-pestaña (Alarmas/Temporizador/Cronómetro) esté el usuario.
    val nextAlarmSubtitle = remember(alarms, now) {
        alarms.filter { it.isEnabled }
            .mapNotNull { AlarmTiming.nextTrigger(it, now = now) }
            .minOrNull()
            ?.let { trigger -> "Próxima alarma en ${AlarmTiming.formatRemaining(trigger - now)}" }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Reloj")
                        if (nextAlarmSubtitle != null) {
                            Text(
                                nextAlarmSubtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.outline,
                            )
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (tabIndex == 0) {
                FloatingActionButton(onClick = onNewAlarm) { Icon(Icons.Default.Add, contentDescription = "Nueva alarma") }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = tabIndex) {
                TABS.forEachIndexed { index, title ->
                    Tab(
                        selected = tabIndex == index,
                        onClick = { coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                        text = { Text(title) },
                    )
                }
            }
            HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { index ->
                when (index) {
                    0 -> AlarmsListScreen(onOpenAlarm = onOpenAlarm, viewModel = alarmsViewModel)
                    1 -> TimerScreen()
                    else -> StopwatchScreen()
                }
            }
        }
    }
}
