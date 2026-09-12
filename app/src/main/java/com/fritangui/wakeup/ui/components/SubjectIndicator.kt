package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fritangui.wakeup.ui.subjects.SubjectIcons

/**
 * Punto de color de siempre si la materia no tiene ícono elegido; si tiene, un avatar circular con
 * ese ícono sobre un fondo tintado del color de la materia — así se reconoce más fácil de un
 * vistazo (#159). Compartido por cualquier sitio donde aparezca una materia o una clase suya:
 * antes solo lo tenía la lista de materias de una carpeta, ahora también Inicio (clase destacada,
 * próximas clases, próximas tareas), la lista de tareas y el selector de materia al crear una tarea
 * (#161).
 */
@Composable
fun SubjectIndicator(color: Color, iconKey: String?, modifier: Modifier = Modifier, size: Dp = 32.dp) {
    val icon = SubjectIcons.iconFor(iconKey)
    if (icon == null) {
        Box(modifier = modifier.size(size).clip(CircleShape).background(color))
    } else {
        Box(
            modifier = modifier.size(size).clip(CircleShape).background(color.copy(alpha = 0.22f)),
            contentAlignment = Alignment.Center,
        ) {
            // Mismo ~56% del diámetro que ya usaba la versión original de este patrón (18dp de 32dp).
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(size * 9 / 16))
        }
    }
}
