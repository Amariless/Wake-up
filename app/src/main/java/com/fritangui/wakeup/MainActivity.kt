package com.fritangui.wakeup

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.fritangui.wakeup.data.datastore.ThemeMode
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
        // installSplashScreen() debe llamarse ANTES de super.onCreate() (así lo pide la librería):
        // muestra el ícono de la app (Theme.WakeUp.Splash → @mipmap/ic_launcher, ver themes.xml)
        // hasta que AppReadyState.isReady se ponga en true, en vez de una pantalla en blanco
        // mientras carga la BD y la primera pantalla.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        splashScreen.setKeepOnScreenCondition { !AppReadyState.isReady.value }
        enableEdgeToEdge()
        // savedInstanceState != null es la señal oficial de Android para "esta activity se está
        // RECREANDO" (rotación, cambio de tema, o el caso que de verdad importaba acá: el proceso
        // murió en segundo plano y el sistema lo recrea reusando el ÚLTIMO Intent que tuvo) — en
        // cualquiera de esos casos, el Intent que llega a este onCreate puede seguir siendo el
        // mismo que abrió un widget hace rato, y sin este chequeo se repetía esa navegación cada
        // vez que se reabría la app, dejando a quien la usa "atrapado" en Bienestar para siempre.
        // Solo se lee el deep link en un onCreate de verdad nuevo (savedInstanceState == null).
        if (savedInstanceState == null) {
            pendingDeepLink.value = WidgetDeepLink.from(intent)
        }
        // Además se limpian los extras ya leídos, como refuerzo: así tampoco quedan puestos por si
        // algo más adelante (no este mismo chequeo) volviera a mirar este Intent.
        intent.replaceExtras(Bundle())
        setContent {
            WakeUpRoot(pendingDeepLink = pendingDeepLink)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingDeepLink.value = WidgetDeepLink.from(intent)
        // Limpia los extras ya consumidos antes de guardar este Intent como el "actual" de la
        // activity: sin esto, si Android mata el proceso más tarde (para liberar memoria, algo
        // normal en apps en segundo plano) y lo recrea reusando este mismo Intent, onCreate volvería
        // a encontrar el deep link del widget y repetiría su navegación — quedando "atrapado" ahí
        // cada vez que se reabre la app, aunque haya pasado mucho tiempo desde que se tocó el widget.
        intent.replaceExtras(Bundle())
        setIntent(intent)
    }
}

@Composable
private fun WakeUpRoot(pendingDeepLink: MutableState<WidgetDeepLink?>, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val dynamicColor by settingsViewModel.dynamicColorEnabled.collectAsState()
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    WakeUpTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
        Surface(modifier = Modifier.fillMaxSize()) {
            WakeUpNavHost(pendingDeepLink = pendingDeepLink)
        }
    }
}
