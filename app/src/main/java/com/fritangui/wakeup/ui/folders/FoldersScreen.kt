@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.data.db.entity.FolderEntity

@Composable
fun FoldersScreen(
    onOpenFolder: (Long) -> Unit,
    viewModel: FoldersViewModel = hiltViewModel(),
) {
    val folders by viewModel.folders.collectAsState()
    val pinnedFolderId by viewModel.pinnedFolderId.collectAsState()
    val pinnedFolderSummary by viewModel.pinnedFolderSummary.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    // La fijada tiene su propia tarjeta destacada más abajo — no hace falta repetirla también como
    // fila plana en la lista de debajo.
    val pinnedFolder = folders.firstOrNull { it.id == pinnedFolderId }
    val restFolders = if (pinnedFolder != null) folders.filterNot { it.id == pinnedFolderId } else folders

    Scaffold(
        topBar = { TopAppBar(title = { Text("Carpetas") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Nueva carpeta")
            }
        },
    ) { padding ->
        if (folders.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Crea tu primera carpeta (semestre, curso, lo que quieras)")
            }
        } else {
            // Al LazyColumn/LazyVerticalGrid le faltaba aplicar `padding` (el que da Scaffold para
            // no quedar tapado por la barra superior/inferior) — con una sola carpeta, esa carpeta
            // quedaba renderizada justo detrás de la TopAppBar, invisible del todo (#5, encontrado
            // con captura del usuario).
            // Grid de 2 columnas (rediseño, antes lista de 1) para el resto de las carpetas — la
            // fijada sigue siendo su propia tarjeta de ancho completo arriba, vía span = maxLineSpan.
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp, 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (pinnedFolder != null) {
                    item(key = "pinned_hero", span = { GridItemSpan(maxLineSpan) }) {
                        PinnedFolderHeroCard(
                            folder = pinnedFolder,
                            summary = pinnedFolderSummary,
                            onClick = { onOpenFolder(pinnedFolder.id) },
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                }
                items(restFolders, key = { it.id }) { folder ->
                    FolderGridCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder.id) },
                        onTerminate = { viewModel.terminateFolder(folder.id) },
                        onReactivate = { viewModel.reactivateFolder(folder.id) },
                        onDelete = { viewModel.deleteFolder(folder) },
                        modifier = Modifier.animateItem(),
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, color ->
                viewModel.createFolder(name, color)
                showCreateDialog = false
            },
        )
    }
}

/** Tarjeta destacada de la carpeta fijada (rediseño): materias y progreso de tareas reales, no
 *  inventados — [PinnedFolderSummary] sale de la misma BD que ya usa el resto de la app. */
@Composable
private fun PinnedFolderHeroCard(folder: FolderEntity, summary: PinnedFolderSummary?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(13.dp))
            Text(
                "CARPETA PRINCIPAL",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(start = 6.dp),
            )
        }
        Text(folder.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
        if (summary != null) {
            Text(
                if (summary.subjectCount == 1) "1 materia" else "${summary.subjectCount} materias",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (summary.totalTasks > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    LinearProgressIndicator(
                        progress = { summary.doneTasks.toFloat() / summary.totalTasks },
                        modifier = Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(999.dp)),
                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        color = MaterialTheme.colorScheme.secondary,
                        strokeCap = androidx.compose.ui.graphics.StrokeCap.Round,
                    )
                    Text(
                        "${summary.doneTasks}/${summary.totalTasks}",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.padding(start = 10.dp),
                    )
                }
            }
        }
    }
}

/** Tarjeta de grid de 2 columnas (rediseño, antes fila de ancho completo): swatch, nombre, estado
 *  (terminada) y el mismo menú de siempre (terminar/reactivar/eliminar), reacomodados en vertical
 *  para el espacio angosto de la columna. */
@Composable
private fun FolderGridCard(
    folder: FolderEntity,
    onClick: () -> Unit,
    onTerminate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmTerminate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(Color(folder.colorArgb), RoundedCornerShape(9.dp)),
            )
            Box(modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menuExpanded = true }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones", modifier = Modifier.size(20.dp))
                }
                DropdownMenu(expanded = menuExpanded, onDismissRequest = { menuExpanded = false }) {
                    if (folder.isActive) {
                        DropdownMenuItem(
                            text = { Text("Terminar semestre") },
                            onClick = { menuExpanded = false; confirmTerminate = true },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Reactivar") },
                            onClick = { menuExpanded = false; onReactivate() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Eliminar") },
                        onClick = { menuExpanded = false; confirmDelete = true },
                    )
                }
            }
        }
        Text(
            text = folder.name,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            textDecoration = if (!folder.isActive) TextDecoration.LineThrough else null,
            modifier = Modifier.padding(top = 10.dp),
        )
        if (!folder.isActive) {
            Row(modifier = Modifier.padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(13.dp),
                    tint = MaterialTheme.colorScheme.outline,
                )
                Text(
                    " Terminada",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
        }
    }

    if (confirmTerminate) {
        AlertDialog(
            onDismissRequest = { confirmTerminate = false },
            title = { Text("¿Terminar \"${folder.name}\"?") },
            text = { Text("Se desactivarán todas sus alarmas y recordatorios de tareas. Los datos no se borran y podrás reactivarla luego.") },
            confirmButton = {
                TextButton(onClick = { confirmTerminate = false; onTerminate() }) { Text("Terminar") }
            },
            dismissButton = { TextButton(onClick = { confirmTerminate = false }) { Text("Cancelar") } },
        )
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("¿Eliminar \"${folder.name}\"?") },
            text = { Text("Se borrarán también sus materias, tareas y alarmas. Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(onClick = { confirmDelete = false; onDelete() }) { Text("Eliminar") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancelar") } },
        )
    }
}

@Composable
private fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String, Int) -> Unit) {
    var name by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(com.fritangui.wakeup.ui.theme.FolderColorPalette.first().toArgb()) }
    val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }

    fun submit() {
        if (name.isNotBlank()) onCreate(name, selectedColor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva carpeta") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre (p.ej. \"Semestre 7\")") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    modifier = Modifier.fillMaxWidth().focusRequester(focusRequester),
                )
                Text("Color", style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 16.dp, bottom = 8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    com.fritangui.wakeup.ui.theme.FolderColorPalette.forEach { color ->
                        val argb = color.toArgb()
                        Box(
                            modifier = Modifier
                                .size(30.dp)
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
            }
        },
        confirmButton = { TextButton(onClick = { submit() }, enabled = name.isNotBlank()) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )

    androidx.compose.runtime.LaunchedEffect(Unit) {
        runCatching { focusRequester.requestFocus() }
    }
}
