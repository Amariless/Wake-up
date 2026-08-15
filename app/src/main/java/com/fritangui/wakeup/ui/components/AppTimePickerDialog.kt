package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Selector de hora estilo MIUI: 3 ruedas lado a lado (AM/PM, hora de 1 a 12, minuto), en vez del
 * `TimePicker` de Material3 (esfera analógica / cajas de texto) que no tenía nada que ver con el
 * resto de los selectores tipo rueda que ya usa la app (horario de clase, temporizador). Por dentro
 * sigue guardando la hora en formato 24h (igual que antes) — el picker solo la muestra distinto.
 *
 * Bottom sheet en vez de diálogo centrado (#150): al ser un selector que se usa con el pulgar
 * (deslizando las ruedas), queda mucho más cómodo pegado abajo de la pantalla que flotando en medio.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    var isPm by remember { mutableStateOf(initialHour >= 12) }
    var hour12 by remember { mutableIntStateOf(to12Hour(initialHour)) }
    var minute by remember { mutableIntStateOf(initialMinute) }
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()

    fun dismiss(after: () -> Unit) {
        scope.launch { sheetState.hide() }.invokeOnCompletion { after() }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Text(
            "Elegir hora",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WheelPicker(
                value = if (isPm) 1 else 0,
                range = 0..1,
                onValueChange = { isPm = it == 1 },
                width = 64.dp,
                label = { if (it == 0) "AM" else "PM" },
            )
            WheelPicker(value = hour12, range = 1..12, onValueChange = { hour12 = it })
            Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 4.dp), textAlign = TextAlign.Center)
            WheelPicker(value = minute, range = 0..59, onValueChange = { minute = it })
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { dismiss(onDismiss) }) { Text("Cancelar") }
            TextButton(onClick = { dismiss { onConfirm(to24Hour(hour12, isPm), minute) } }) { Text("Aceptar") }
        }
    }
}

private fun to12Hour(hour24: Int): Int {
    val h = hour24 % 12
    return if (h == 0) 12 else h
}

private fun to24Hour(hour12: Int, isPm: Boolean): Int {
    val h = hour12 % 12
    return if (isPm) h + 12 else h
}
