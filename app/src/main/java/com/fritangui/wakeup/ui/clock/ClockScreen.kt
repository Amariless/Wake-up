@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.clock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
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
fun ClockScreen(
    onOpenAlarm: (Long) -> Unit,
    onNewAlarm: () -> Unit,
    /** Se incrementa desde la acción rápida "Enfoque" de Inicio: salta a la sub-pestaña
     *  Temporizador, igual que scrollToNextClassSignal salta el scroll de Inicio (ver WakeUpNavHost). */
    jumpToTimerTabSignal: Int = 0,
) {
    // #145: antes solo se podía cambiar de sub-pestaña tocándola; con HorizontalPager también se
    // puede deslizar hacia el lado. pagerState ya persiste la página seleccionada (rememberSaveable
    // por dentro), así que reemplaza al `tabIndex` suelto de antes.
    val pagerState = rememberPagerState(pageCount = { TABS.size })
    val coroutineScope = rememberCoroutineScope()
    val tabIndex = pagerState.currentPage

    LaunchedEffect(jumpToTimerTabSignal) {
        if (jumpToTimerTabSignal > 0) pagerState.animateScrollToPage(1)
    }
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
            SegmentedTabRow(
                tabs = TABS,
                selectedIndex = tabIndex,
                onSelected = { index -> coroutineScope.launch { pagerState.animateScrollToPage(index) } },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
            )
            // weight(1f), NO fillMaxSize(): ver el mismo arreglo en FolderDetailScreen (bug de
            // "espacio vacío" que empujaba las alarmas/recordatorios hacia abajo).
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
                when (index) {
                    0 -> AlarmsListScreen(onOpenAlarm = onOpenAlarm, viewModel = alarmsViewModel)
                    1 -> TimerScreen()
                    else -> StopwatchScreen()
                }
            }
        }
    }
}

/** Control segmentado en pastilla (fondo `surfaceVariant`, ítem activo en `surface` con sombra) en
 *  vez del TabRow con subrayado de Material — mismo lenguaje visual que la barra de navegación. */
@Composable
private fun SegmentedTabRow(tabs: List<String>, selectedIndex: Int, onSelected: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
    ) {
        tabs.forEachIndexed { index, title ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(999.dp))
                    .then(
                        if (isSelected) {
                            Modifier
                                .shadow(elevation = 2.dp, shape = RoundedCornerShape(999.dp), clip = false)
                                .background(MaterialTheme.colorScheme.surface)
                        } else {
                            Modifier
                        },
                    )
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelected(index)
                    }
                    .padding(vertical = 9.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
                )
            }
        }
    }
}
