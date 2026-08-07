@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.blocking

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Pantalla de depuración (accesible desde el panel de desarrollador): muestra
 * en vivo los resource-id detectados por [ReelsBlockAccessibilityService] en
 * la última ventana inspeccionada. Sirve para recalibrar [ReelsNodeDetector]
 * si Instagram/TikTok cambian sus ids en una actualización — abre la app
 * objetivo, entra a Reels/DMs y compara qué ids aparecen aquí.
 */
@Composable
fun NodeInspectorScreen() {
    val detection by ReelsBlockAccessibilityService.lastDetection.collectAsState()
    val isRunning by ReelsBlockAccessibilityService.isRunning.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Inspector de nodos") }) }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text(if (isRunning) "Servicio de accesibilidad: activo" else "Servicio de accesibilidad: inactivo")
            Text("Última app inspeccionada: ${detection?.packageName ?: "—"}")
            Text("Superficie detectada: ${detection?.surface?.name ?: "ninguna"}")
            Text("IDs vistos (${detection?.matchedIds?.size ?: 0}):", style = MaterialTheme.typography.titleMedium)
            LazyColumn {
                items(detection?.matchedIds?.toList() ?: emptyList()) { id ->
                    Text(id, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
