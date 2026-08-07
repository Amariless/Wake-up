package com.fritangui.wakeup.alarm.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Botón "Silenciar 20s": le da al usuario un respiro para concentrarse en el
 * reto sin el ruido de la alarma/temporizador encima. Si el reto no se
 * completa (la pantalla no se cierra) antes de que se acabe la cuenta, el
 * propio servicio vuelve a sonar solo — este composable solo manda la orden y
 * muestra la cuenta regresiva, no controla el sonido directamente.
 */
@Composable
fun MuteTemporarilyButton(onMute: () -> Unit) {
    var remainingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1_000)
            remainingSeconds--
        }
    }

    OutlinedButton(
        onClick = {
            onMute()
            remainingSeconds = 20
        },
        enabled = remainingSeconds == 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (remainingSeconds > 0) "Silenciado ${remainingSeconds}s…" else "Silenciar 20s")
    }
}
