package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Material3 no incluye un diálogo de hora listo para usar, solo el componente [TimePicker]; este lo envuelve. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit,
) {
    val state = rememberTimePickerState(initialHour = initialHour, initialMinute = initialMinute, is24Hour = true)
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { TimePicker(state = state, modifier = Modifier.padding(top = 8.dp)) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) { androidx.compose.material3.Text("Aceptar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { androidx.compose.material3.Text("Cancelar") } },
    )
}
