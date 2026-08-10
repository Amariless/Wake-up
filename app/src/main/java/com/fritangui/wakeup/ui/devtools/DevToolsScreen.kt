@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.fritangui.wakeup.ui.devtools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.blocking.NodeInspectorScreen

/**
 * Solo disponible en builds debug. Ver `README.md` → "Probar en modo dev"
 * para el flujo completo de uso de este panel.
 */
@Composable
fun DevToolsScreen(viewModel: DevToolsViewModel = hiltViewModel()) {
    Scaffold(topBar = { TopAppBar(title = { Text("Panel de desarrollador") }) }) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp)) {
            Text("Solo visible en builds debug. Para probar sin esperar horarios reales.")
            DevButton("Disparar alarma ya mismo") { viewModel.fireAlarmNow() }
            DevButton("Simular notificación T-60") { viewModel.simulatePreAlarmNotification() }
            Text(
                "⚠️ Esto crea una carpeta \"Demo 2026-1\" real y visible en tu horario, no un preview — bórrala con el botón de abajo cuando termines de probar.",
                modifier = Modifier.padding(top = 12.dp),
            )
            DevButton("Poblar datos de ejemplo (carpeta+materia+tarea+alarma)") { viewModel.seedDemoData() }
            DevButton("Borrar datos de ejemplo (\"Demo 2026-1\")") { viewModel.deleteDemoData() }
            DevButton("Forzar overlay de bloqueo") { viewModel.forceBlockOverlay() }
            Text("Inspector de nodos de accesibilidad", modifier = Modifier.padding(top = 24.dp, bottom = 8.dp))
            NodeInspectorScreen()
        }
    }
}

@Composable
private fun DevButton(text: String, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) { Text(text) }
}
