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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.fritangui.wakeup.alarm.TimerForegroundService
import com.fritangui.wakeup.data.db.entity.DismissChallengeType
import kotlinx.coroutines.flow.drop

@Composable
fun TimerRingingScreen(onDismissed: () -> Unit) {
    val context = LocalContext.current
    val state by TimerForegroundService.state.collectAsState()

    // Si el temporizador se detiene por una vía externa a esta pantalla (el watchdog,
    // un cambio de estado inesperado, etc.), esta pantalla no debe quedarse congelada
    // mostrando "¡Tiempo!" para siempre: se cierra sola apenas isRinging pasa a false.
    LaunchedEffect(Unit) {
        snapshotFlow { state.isRinging }
            .drop(1)
            .collect { isRinging -> if (!isRinging) onDismissed() }
    }

    val challenge = state.challenge

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
            DismissChallengeContent(challenge, state.difficulty, onDismissed)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (challenge == DismissChallengeType.NONE) {
                Button(onClick = onDismissed, modifier = Modifier.fillMaxWidth()) { Text("Apagar") }
            } else {
                MuteTemporarilyButton(onMute = {
                    context.startService(TimerForegroundService.actionIntent(context, TimerForegroundService.ACTION_MUTE_TEMPORARILY))
                })
            }
        }
    }
}
