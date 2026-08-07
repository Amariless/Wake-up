package com.fritangui.wakeup.alarm.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fritangui.wakeup.alarm.TimerForegroundService
import com.fritangui.wakeup.data.db.entity.DismissChallengeType

@Composable
fun TimerRingingScreen(onDismissed: () -> Unit) {
    val state by TimerForegroundService.state.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(top = 48.dp)) {
            Text("¡Tiempo!", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Bold)
            Text("El temporizador terminó", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
        }

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DismissChallengeContent(state.challenge, state.difficulty, onDismissed)
        }

        if (state.challenge == DismissChallengeType.NONE) {
            Button(onClick = onDismissed, modifier = Modifier.fillMaxWidth()) { Text("Apagar") }
        } else {
            Box(modifier = Modifier.fillMaxWidth())
        }
    }
}
