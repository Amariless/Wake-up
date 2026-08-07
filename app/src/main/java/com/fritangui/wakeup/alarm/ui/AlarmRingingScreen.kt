package com.fritangui.wakeup.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

@Composable
fun AlarmRingingScreen(
    alarmId: Long,
    onDismissed: () -> Unit,
    onSnoozed: () -> Unit,
    viewModel: AlarmRingingViewModel = hiltViewModel(),
) {
    LaunchedEffect(alarmId) { viewModel.load(alarmId) }
    val alarm by viewModel.alarm.collectAsState()
    val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    val challenge = alarm?.dismissChallenge ?: DismissChallengeType.NONE

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 32.dp)) {
            Text(
                text = "%02d:%02d".format(now.hour, now.minute),
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = alarm?.label?.ifBlank { "Alarma" } ?: "Alarma",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DismissChallengeContent(challenge, alarm?.challengeDifficulty ?: 1, onDismissed)
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (challenge == DismissChallengeType.NONE) {
                Button(onClick = onDismissed, modifier = Modifier.fillMaxWidth()) { Text("Apagar") }
            }
            OutlinedButton(onClick = onSnoozed, modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                Text("Posponer 5 minutos")
            }
        }
    }
}
