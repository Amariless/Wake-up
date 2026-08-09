@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.update

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.BuildConfig
import com.fritangui.wakeup.permissions.PermissionIntents
import com.fritangui.wakeup.permissions.PermissionStatus
import com.fritangui.wakeup.update.DownloadState
import com.fritangui.wakeup.update.UpdateCheckState

@Composable
fun UpdateScreen(onBack: () -> Unit, viewModel: UpdateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val checkState by viewModel.checkState.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    var canInstall by remember { mutableStateOf(PermissionStatus.canInstallPackages(context)) }

    LaunchedEffect(Unit) { viewModel.checkForUpdate() }

    // Al volver de la pantalla de "permitir instalar apps desconocidas", revisa de nuevo.
    LaunchedEffect(downloadState) {
        canInstall = PermissionStatus.canInstallPackages(context)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buscar actualizaciones") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Versión instalada: ${BuildConfig.VERSION_NAME}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Repositorio: ${BuildConfig.GITHUB_REPO}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline,
                modifier = Modifier.padding(bottom = 24.dp),
            )

            when (val state = checkState) {
                UpdateCheckState.Idle, UpdateCheckState.Checking -> {
                    CircularProgressIndicator()
                    Text("Buscando en GitHub…", modifier = Modifier.padding(top = 12.dp))
                }
                UpdateCheckState.UpToDate -> {
                    Text("Ya tienes la última versión ✅", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = viewModel::checkForUpdate, modifier = Modifier.padding(top = 8.dp)) { Text("Revisar de nuevo") }
                }
                is UpdateCheckState.Error -> {
                    Text("No se pudo revisar: ${state.message}", color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::checkForUpdate, modifier = Modifier.padding(top = 8.dp)) { Text("Reintentar") }
                }
                is UpdateCheckState.Available -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Hay una actualización disponible", style = MaterialTheme.typography.titleMedium)
                            Text(state.info.releaseName, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${state.info.fileSizeBytes / (1024 * 1024)} MB",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline,
                            )

                            when (val dl = downloadState) {
                                DownloadState.Idle -> {
                                    if (!canInstall) {
                                        Text(
                                            "Primero permite instalar apps desde Wake up.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.padding(top = 8.dp),
                                        )
                                        TextButton(onClick = { PermissionIntents.safeStart(context, PermissionIntents.manageUnknownAppSources(context)) }) {
                                            Text("Permitir instalación")
                                        }
                                    } else {
                                        Button(onClick = viewModel::startDownload, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                            Text("Descargar e instalar")
                                        }
                                    }
                                }
                                is DownloadState.Downloading -> {
                                    LinearProgressIndicator(
                                        progress = { dl.progressPercent / 100f },
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    )
                                    Text("${dl.progressPercent}%", style = MaterialTheme.typography.bodySmall)
                                }
                                is DownloadState.ReadyToInstall -> {
                                    Button(
                                        onClick = { viewModel.installIntent()?.let { context.startActivity(it) } },
                                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                                    ) { Text("Instalar ahora") }
                                }
                                is DownloadState.Failed -> {
                                    Text(dl.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                                    TextButton(onClick = viewModel::startDownload) { Text("Reintentar descarga") }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
