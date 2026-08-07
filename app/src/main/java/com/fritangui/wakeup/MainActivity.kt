package com.fritangui.wakeup

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.ui.WakeUpNavHost
import com.fritangui.wakeup.ui.settings.SettingsViewModel
import com.fritangui.wakeup.ui.theme.WakeUpTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app (patrón single-activity + Navigation-Compose).
 * Aloja todas las pantallas de folders/materias/tareas/reloj/etc. La activity
 * de alarma sonando ([com.fritangui.wakeup.alarm.AlarmRingingActivity]) es una
 * activity aparte porque necesita mostrarse sobre la pantalla de bloqueo
 * incluso si esta activity no está en primer plano.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WakeUpRoot()
        }
    }
}

@Composable
private fun WakeUpRoot(settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val dynamicColor by settingsViewModel.dynamicColorEnabled.collectAsState()
    WakeUpTheme(dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            WakeUpNavHost()
        }
    }
}
