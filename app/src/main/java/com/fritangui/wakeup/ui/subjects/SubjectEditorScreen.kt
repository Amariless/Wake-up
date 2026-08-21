@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

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
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.runtime.DisposableEffect
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
import com.fritangui.wakeup.ui.navigation.UnsavedChangesGuard
import com.fritangui.wakeup.ui.tasks.TaskListColumn
import com.fritangui.wakeup.ui.theme.SubjectColorPalette

private val DIA_NOMBRES = listOf("Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom")

@Composable
fun SubjectEditorScreen(
    onBack: () -> Unit,
    onOpenSession: (folderId: Long, subjectId: Long, sessionId: Long) -> Unit,
    onOpenTask: (folderId: Long, taskId: Long) -> Unit,
    viewModel: SubjectEditorViewModel = hiltViewModel(),
) {
    val subject by viewModel.subject.collectAsState()
    val currentSubjectId by viewModel.currentSubjectId.collectAsState()
    val sessions by viewModel.sessions.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    val defaultColor = remember { SubjectColorPalette.first().toArgb() }
    var name by rememberSaveable(subject?.id) { mutableStateOf(subject?.name ?: "") }
    var professor by rememberSaveable(subject?.id) { mutableStateOf(subject?.professor ?: "") }
    var selectedColor by remember(subject?.id) { mutableStateOf(subject?.colorArgb ?: defaultColor) }
    var selectedIcon by rememberSaveable(subject?.id) { mutableStateOf(subject?.iconKey) }
    var confirmDiscard by remember { mutableStateOf(false) }
    var pendingLeaveAction by remember { mutableStateOf<(() -> Unit)?>(null) }

    // Hay cambios sin guardar si algo difiere de lo último que llegó de la BD (o, para una
    // materia nueva que aún no existe, si el usuario ya escribió algo).
    val isDirty = name != (subject?.name ?: "") ||
        professor != (subject?.professor ?: "") ||
        selectedColor != (subject?.colorArgb ?: defaultColor) ||
        selectedIcon != subject?.iconKey

    fun requestLeave(action: () -> Unit) {
        if (isDirty) {
            pendingLeaveAction = action
            confirmDiscard = true
        } else {
            action()
        }
    }
    fun tryExit() = requestLeave(onBack)

    BackHandler(onBack = ::tryExit)
    // Se registra en el "puente" compartido con la barra de navegación inferior: si el usuario
    // toca otro tab con cambios sin guardar acá, esto deja que ESTE diálogo se muestre primero en
    // vez de que la navegación descarte los cambios de una (#147).
    DisposableEffect(isDirty) {
        UnsavedChangesGuard.register(isDirty, ::requestLeave)
        onDispose { UnsavedChangesGuard.clear() }
    }

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
                            viewModel.saveBasicInfo(name, professor, selectedColor, selectedIcon) { if (wasExisting) onBack() }
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
            // FlowRow en vez de Row: con 16 colores (antes 8) ya no caben todos en una sola fila.
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SubjectColorPalette.forEach { color ->
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

            // Ícono opcional para reconocer la materia más fácil de un vistazo (además del color).
            Text("Ícono (opcional)", modifier = Modifier.padding(top = 16.dp, bottom = 4.dp))
            androidx.compose.foundation.layout.FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // "Sin ícono": vuelve a mostrar solo el punto de color, como antes de esta función.
                IconPickerCell(icon = null, isSelected = selectedIcon == null, tint = selectedColor) { selectedIcon = null }
                SubjectIcons.catalog.forEach { (key, icon) ->
                    IconPickerCell(icon = icon, isSelected = selectedIcon == key, tint = selectedColor) { selectedIcon = key }
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

                HorizontalDivider(modifier = Modifier.padding(vertical = 20.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Tareas", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        viewModel.presetNewTaskToThisSubject()
                        onOpenTask(viewModel.folderId, 0L)
                    }) { Text("+ Agregar") }
                }
                if (tasks.isEmpty()) {
                    Text(
                        "Esta materia todavía no tiene tareas.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    TaskListColumn(
                        tasks = tasks,
                        onToggle = viewModel::setTaskCompleted,
                        onClick = { onOpenTask(viewModel.folderId, it) },
                        onDelete = viewModel::deleteTask,
                        onCompleteWithGrade = viewModel::completeTaskWithGrade,
                    )
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
                TextButton(onClick = { confirmDiscard = false; pendingLeaveAction?.invoke() }) { Text("Salir sin guardar") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Seguir editando") } },
        )
    }
}

/** Una celda del selector de íconos: `icon == null` es la opción "sin ícono" (una X). */
@Composable
private fun IconPickerCell(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    isSelected: Boolean,
    tint: Int,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (isSelected) androidx.compose.ui.graphics.Color(tint).copy(alpha = 0.25f) else MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(if (isSelected) Modifier.border(2.dp, androidx.compose.ui.graphics.Color(tint), CircleShape) else Modifier)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = androidx.compose.ui.graphics.Color(tint), modifier = Modifier.size(22.dp))
        } else {
            Icon(Icons.Default.Close, contentDescription = "Sin ícono", tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(18.dp))
        }
    }
}
