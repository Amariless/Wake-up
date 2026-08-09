@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.subjects

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.ui.theme.FolderColorPalette

private val DIA_NOMBRES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@Composable
fun SubjectEditorScreen(
    onBack: () -> Unit,
    onOpenSession: (folderId: Long, subjectId: Long, sessionId: Long) -> Unit,
    viewModel: SubjectEditorViewModel = hiltViewModel(),
) {
    val subject by viewModel.subject.collectAsState()
    val currentSubjectId by viewModel.currentSubjectId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()

    val defaultColor = remember { FolderColorPalette.first().toArgb() }
    var name by rememberSaveable(subject?.id) { mutableStateOf(subject?.name ?: "") }
    var professor by rememberSaveable(subject?.id) { mutableStateOf(subject?.professor ?: "") }
    var selectedColor by remember(subject?.id) { mutableStateOf(subject?.colorArgb ?: defaultColor) }
    var confirmDiscard by remember { mutableStateOf(false) }

    // Hay cambios sin guardar si algo difiere de lo último que llegó de la BD (o, para una
    // materia nueva que aún no existe, si el usuario ya escribió algo).
    val isDirty = name != (subject?.name ?: "") ||
        professor != (subject?.professor ?: "") ||
        selectedColor != (subject?.colorArgb ?: defaultColor)

    fun tryExit() {
        if (isDirty) confirmDiscard = true else onBack()
    }

    BackHandler(onBack = ::tryExit)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (currentSubjectId == null) "Nueva materia" else "Editar materia",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = { IconButton(onClick = ::tryExit) { Icon(Icons.AutoMirrored.Filled.ArrowBack, null) } },
                actions = {
                    TextButton(
                        onClick = {
                            // Al editar una materia que ya existe, "Guardar" cierra el editor (ya no
                            // queda nada pendiente ahí). Al crearla por primera vez, en cambio, nos
                            // quedamos en la pantalla para poder agregar sus horarios de una: si
                            // saliéramos de una vez, habría que volver a entrar a mano para eso.
                            val wasExisting = currentSubjectId != null
                            viewModel.saveBasicInfo(name, professor, selectedColor) { if (wasExisting) onBack() }
                        },
                        enabled = name.isNotBlank(),
                    ) { Text(if (currentSubjectId == null) "Crear" else "Guardar") }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp).verticalScroll(rememberScrollState())) {
            Text("Información básica", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre de la materia") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            OutlinedTextField(
                value = professor,
                onValueChange = { professor = it },
                label = { Text("Profesor (opcional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            )
            Text("Color", modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FolderColorPalette.forEach { color ->
                    val argb = color.toArgb()
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color)
                            .then(
                                if (argb == selectedColor) {
                                    Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                } else {
                                    Modifier
                                },
                            )
                            .clickable { selectedColor = argb },
                    )
                }
            }

            if (currentSubjectId != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Horarios", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = { onOpenSession(viewModel.folderId, currentSubjectId!!, 0L) }) { Text("+ Agregar") }
                }
                if (sessions.isEmpty()) {
                    Text(
                        "Aún no tiene horario. Puedes agregar el mismo horario para varios días a la vez.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Column(modifier = Modifier.padding(top = 8.dp)) {
                    sessions.forEach { session ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp).clickable {
                                    onOpenSession(viewModel.folderId, currentSubjectId!!, session.id)
                                },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "${DIA_NOMBRES[session.dayOfWeek - 1]} · " +
                                        "%02d:%02d–%02d:%02d".format(
                                            session.startMinuteOfDay / 60, session.startMinuteOfDay % 60,
                                            session.endMinuteOfDay / 60, session.endMinuteOfDay % 60,
                                        ) + " · ${session.room}",
                                )
                                IconButton(onClick = { viewModel.deleteSession(session) }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar horario")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("¿Salir sin guardar?") },
            text = { Text("Tienes cambios sin guardar en esta materia. Si sales ahora se pierden.") },
            confirmButton = {
                TextButton(onClick = { confirmDiscard = false; onBack() }) { Text("Salir sin guardar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } },
        )
    }
}
