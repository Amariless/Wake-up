@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.subjects

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavBackStackEntry
import com.fritangui.wakeup.data.db.entity.ClassSessionEntity
import com.fritangui.wakeup.ui.components.WheelTimePicker

private val DIA_NOMBRES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

/**
 * Pantalla completa (no diálogo flotante) para crear/editar un horario de clase. Antes era un
 * [androidx.compose.material3.AlertDialog], pero con las ruedas de hora más grandes (#38) ya no
 * cabía todo el contenido: el campo "Salón" quedaba empujado fuera del recuadro y parecía que no
 * se podía guardar. Como pantalla propia hay todo el espacio que haga falta.
 */
@Composable
fun SessionEditorScreen(
    backStackEntry: NavBackStackEntry,
    onBack: () -> Unit,
    viewModel: SubjectEditorViewModel = hiltViewModel(),
) {
    val sessionId = backStackEntry.arguments?.getLong("sessionId") ?: 0L
    val sessions by viewModel.sessions.collectAsState()
    val initial = remember(sessions, sessionId) { sessions.firstOrNull { it.id == sessionId } }

    // Al crear un horario nuevo se pueden elegir varios días a la vez (misma hora/salón, p.ej. una
    // materia que se ve martes y jueves); al editar uno ya existente se queda fijo a ese día.
    // Todo queda con `initial` como key: cuando se está editando, la primera composición llega
    // antes de que el Flow de Room traiga el horario real (arranca en null), así que si no se
    // vuelve a inicializar apenas llega el valor real, se quedarían los campos vacíos por defecto.
    var selectedDays by remember(initial) { mutableStateOf(setOf(initial?.dayOfWeek ?: 1)) }
    var startHour by remember(initial) { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) / 60) }
    var startMinute by remember(initial) { mutableStateOf((initial?.startMinuteOfDay ?: 7 * 60) % 60) }
    var endHour by remember(initial) { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) / 60) }
    var endMinute by remember(initial) { mutableStateOf((initial?.endMinuteOfDay ?: 9 * 60) % 60) }
    var room by rememberSaveable(initial) { mutableStateOf(initial?.room ?: "") }

    // Al mover la hora de inicio (por interacción del usuario, no al abrir la pantalla),
    // la de fin se adelanta sola 2 horas — se puede seguir ajustando a mano después.
    fun applyStart(newHour: Int, newMinute: Int) {
        startHour = newHour
        startMinute = newMinute
        val totalEnd = (newHour * 60 + newMinute + 120) % (24 * 60)
        endHour = totalEnd / 60
        endMinute = totalEnd % 60
    }

    fun save() {
        val newSessions = selectedDays.sorted().map { day ->
            ClassSessionEntity(
                id = if (initial != null && day == initial.dayOfWeek) initial.id else 0L,
                subjectId = initial?.subjectId ?: 0L,
                dayOfWeek = day,
                startMinuteOfDay = startHour * 60 + startMinute,
                endMinuteOfDay = endHour * 60 + endMinute,
                room = room.trim(),
            )
        }
        newSessions.forEach { viewModel.addOrUpdateSession(it) }
        onBack()
    }

    val canSave = room.isNotBlank() && selectedDays.isNotEmpty() && (endHour * 60 + endMinute) > (startHour * 60 + startMinute)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initial == null) "Nuevo horario" else "Editar horario") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(onClick = ::save, enabled = canSave) { Text("Guardar") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                if (initial == null) "Día(s)" else "Día",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.fillMaxWidth().padding(top = 20.dp),
                textAlign = TextAlign.Center,
            )
            // Sin cajas ni chips: cada día es solo texto que cambia de color y peso al
            // seleccionarse, para no competir en espacio con nada más de la pantalla.
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                DIA_NOMBRES.forEachIndexed { index, label ->
                    val iso = index + 1
                    val isSelected = iso in selectedDays
                    Text(
                        label.take(1),
                        style = if (isSelected) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        modifier = Modifier.clickable {
                            selectedDays = if (initial != null) {
                                setOf(iso)
                            } else if (iso in selectedDays) {
                                (selectedDays - iso).ifEmpty { setOf(iso) }
                            } else {
                                selectedDays + iso
                            }
                        }.padding(12.dp),
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))

            // Arriba de las ruedas de hora a propósito: quedaba al final, así que al enfocarlo el
            // teclado lo tapaba (y encima había que hacer scroll para encontrarlo).
            OutlinedTextField(
                value = room,
                onValueChange = { room = it },
                label = { Text("Salón") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Text("Inicio", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 20.dp))
            WheelTimePicker(
                hour = startHour,
                minute = startMinute,
                onHourChange = { applyStart(it, startMinute) },
                onMinuteChange = { applyStart(startHour, it) },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )

            Text("Fin", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(top = 16.dp))
            WheelTimePicker(
                hour = endHour,
                minute = endMinute,
                onHourChange = { endHour = it },
                onMinuteChange = { endMinute = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}
