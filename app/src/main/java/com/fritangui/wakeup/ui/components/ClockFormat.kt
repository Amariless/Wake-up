package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

/**
 * De dónde sale, en cualquier composable de la app, si hay que mostrar la hora en 12h (con AM/PM)
 * o en 24h — se llena una sola vez cerca de la raíz de la navegación (ver WakeUpNavHost) leyendo
 * [com.fritangui.wakeup.data.datastore.SettingsDataStore.use24HourFormat], para no tener que
 * inyectar el DataStore en cada pantalla que necesita mostrar una hora.
 */
val LocalUse24HourFormat = compositionLocalOf { false }

/** "6:22" (12h, sin ceros a la izquierda en la hora) o "18:22" (24h) — sin AM/PM, para incrustar en frases. */
fun formatClockTime(hour: Int, minute: Int, use24Hour: Boolean): String {
    return if (use24Hour) {
        "%02d:%02d".format(hour, minute)
    } else {
        val h12 = hour % 12
        "%d:%02d".format(if (h12 == 0) 12 else h12, minute)
    }
}

/** null en 24h (no aplica); "AM"/"PM" en 12h. */
fun amPmSuffix(hour: Int, use24Hour: Boolean): String? = if (use24Hour) null else if (hour < 12) "AM" else "PM"

/**
 * Composable para mostrar una hora de forma prominente (el número grande del editor de alarma, la
 * hora de cada fila en la lista de alarmas): en 12h agrega el AM/PM en un tamaño bastante más
 * chico al lado, en vez de mezclarlo al mismo tamaño que el resto del texto.
 */
@Composable
fun ClockTimeText(
    hour: Int,
    minute: Int,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    modifier: Modifier = Modifier,
) {
    val use24Hour = LocalUse24HourFormat.current
    val suffix = amPmSuffix(hour, use24Hour)
    if (suffix == null) {
        Text(formatClockTime(hour, minute, true), style = style, color = color, modifier = modifier)
    } else {
        Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
            Text(formatClockTime(hour, minute, false), style = style, color = color)
            Spacer(modifier = Modifier.width(4.dp))
            Text(suffix, style = style.copy(fontSize = style.fontSize * 0.42f), color = color)
        }
    }
}
