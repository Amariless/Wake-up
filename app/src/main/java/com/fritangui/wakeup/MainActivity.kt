package com.fritangui.wakeup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.fritangui.wakeup.ui.WakeUpNavHost
import com.fritangui.wakeup.ui.settings.SettingsViewModel
import com.fritangui.wakeup.ui.theme.WakeUpTheme
import com.fritangui.wakeup.widget.WidgetDeepLink
import dagger.hilt.android.AndroidEntryPoint

/**
 * Única Activity de la app (patrón single-activity + Navigation-Compose).
 * Aloja todas las pantallas de folders/materias/tareas/reloj/etc. La activity
 * de alarma sonando ([com.fritangui.wakeup.alarm.AlarmRingingActivity]) es una
 * activity aparte porque necesita mostrarse sobre la pantalla de bloqueo
 * incluso si esta activity no está en primer plano.
 *
 * `singleTask` en el manifest: si ya está abierta y se toca un widget, no se
 * vuelve a crear — llega por [onNewIntent] en vez de [onCreate], así que el
 * deep link se expone como un [MutableState] que ambos puntos de entrada
 * actualizan por igual.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // Se inicializa en null a propósito: `intent` todavía no está listo en el constructor (lo fija
    // el framework después, antes de onCreate) — se llena de verdad en onCreate.
    private val pendingDeepLink = mutableStateOf<WidgetDeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingDeepLink.value = WidgetDeepLink.from(intent)
        setContent {
            WakeUpRoot(pendingDeepLink = pendingDeepLink)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        pendingDeepLink.value = WidgetDeepLink.from(intent)
    }
}

@Composable
private fun WakeUpRoot(pendingDeepLink: MutableState<WidgetDeepLink?>, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val dynamicColor by settingsViewModel.dynamicColorEnabled.collectAsState()
    WakeUpTheme(dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            WakeUpNavHost(pendingDeepLink = pendingDeepLink)
        }
    }
}
