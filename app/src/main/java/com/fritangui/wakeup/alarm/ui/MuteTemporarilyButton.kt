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
 * Botón "Bajar volumen 1 min": le da al usuario un respiro para concentrarse
 * en el reto sin el ruido de la alarma/temporizador tan encima — pero no la
 * deja en silencio total, y si en ese minuto no se resolvió el reto (la
 * pantalla no se cerró), el propio servicio vuelve a subirla sola. Este
 * composable solo manda la orden y muestra la cuenta regresiva, no controla
 * el volumen directamente.
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
            remainingSeconds = 60
        },
        enabled = remainingSeconds == 0,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (remainingSeconds > 0) "Volumen bajo, ${remainingSeconds}s…" else "Bajar volumen 1 min")
    }
}
