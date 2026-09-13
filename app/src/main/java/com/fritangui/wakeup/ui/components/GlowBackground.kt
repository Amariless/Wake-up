package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Resplandor radial suave centrado (#161), como el del mockup de referencia para Temporizador y
 * Cronómetro: un tinte del [color] de acento, muy tenue, que se apaga hacia los bordes de la
 * pantalla. Pensado para fondo oscuro — sobre un fondo claro el efecto queda casi imperceptible
 * a propósito, no tiene sentido ahí.
 */
@Composable
fun Modifier.glowBackground(color: Color = MaterialTheme.colorScheme.primary): Modifier =
    this.background(Brush.radialGradient(colors = listOf(color.copy(alpha = 0.22f), Color.Transparent)))
