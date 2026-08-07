package com.fritangui.wakeup.ui.clock.alarms

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.AlarmEntity

private val DIA_LETRAS = listOf("L", "M", "X", "J", "V", "S", "D")

@Composable
fun AlarmsListScreen(
    onOpenAlarm: (Long) -> Unit,
    viewModel: AlarmsViewModel = hiltViewModel(),
) {
    val alarms by viewModel.alarms.collectAsState()

    if (alarms.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
            Text("Toca + para crear tu primera alarma del reloj general")
        }
        return
    }

    LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
        items(alarms, key = { it.id }) { alarm ->
            AlarmRow(alarm, onClick = { onOpenAlarm(alarm.id) }, onToggle = { viewModel.setEnabled(alarm.id, it) })
        }
    }
}

@Composable
private fun AlarmRow(alarm: AlarmEntity, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text("%02d:%02d".format(alarm.hour, alarm.minute), style = MaterialTheme.typography.headlineMedium)
                Text(alarm.label.ifBlank { "Alarma" }, style = MaterialTheme.typography.bodyMedium)
                Text(repeatSummary(alarm.repeatDaysBitmask), style = MaterialTheme.typography.bodyMedium)
            }
            Switch(checked = alarm.isEnabled, onCheckedChange = onToggle)
        }
    }
}

private fun repeatSummary(bitmask: Int): String {
    if (bitmask == 0) return "Una vez"
    if (bitmask == 0b1111111) return "Todos los días"
    if (bitmask == 0b0011111) return "Lunes a viernes"
    return (1..7).filter { (bitmask and AlarmEntity.dayBit(it)) != 0 }.joinToString(" ") { DIA_LETRAS[it - 1] }
}
