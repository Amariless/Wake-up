@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.folders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    var showCreateDialog by remember { mutableStateOf(false) }

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
            LazyColumn(contentPadding = PaddingValues(16.dp, 8.dp)) {
                items(folders, key = { it.id }) { folder ->
                    FolderRow(
                        folder = folder,
                        onClick = { onOpenFolder(folder.id) },
                        onTerminate = { viewModel.terminateFolder(folder.id) },
                        onReactivate = { viewModel.reactivateFolder(folder.id) },
                        onDelete = { viewModel.deleteFolder(folder) },
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateFolderDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = {
                viewModel.createFolder(it)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun FolderRow(
    folder: FolderEntity,
    onClick: () -> Unit,
    onTerminate: () -> Unit,
    onReactivate: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var confirmTerminate by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).clickable(onClick = onClick)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .background(Color(folder.colorArgb), CircleShape),
            )
            Column(modifier = Modifier.padding(start = 12.dp).weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleLarge,
                    textDecoration = if (!folder.isActive) TextDecoration.LineThrough else null,
                )
                if (!folder.isActive) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.outline,
                        )
                        Text(
                            " Terminada · solo lectura",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline,
                        )
                    }
                }
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Más opciones")
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
private fun CreateFolderDialog(onDismiss: () -> Unit, onCreate: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nueva carpeta") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre (p.ej. \"2026-1\")") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = { TextButton(onClick = { onCreate(name) }, enabled = name.isNotBlank()) { Text("Crear") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
