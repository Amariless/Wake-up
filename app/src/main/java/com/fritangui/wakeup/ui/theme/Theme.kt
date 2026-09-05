package com.fritangui.wakeup.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Paleta clara del rediseño 2026 ("Minimalismo táctil"): fondo cálido neutro + acentos pastel-
// saturados. `outline`/`outlineVariant` no se usan aquí con su sentido literal de Material (borde):
// el resto de la app ya usaba `colorScheme.outline` como color de texto secundario/atenuado, así
// que se mapea a WakeUpTextSecondary para que ese patrón siga funcionando en cada pantalla sin
// tocarla una por una; WakeUpBorder (el borde real de tarjetas/chips) vive en outlineVariant.
private val LightColors = lightColorScheme(
    primary = WakeUpIndigo,
    onPrimary = Color.White,
    primaryContainer = WakeUpIndigo.copy(alpha = 0.14f).compositeOver(WakeUpSurface),
    onPrimaryContainer = WakeUpIndigo,
    secondary = WakeUpCoral,
    onSecondary = Color.White,
    secondaryContainer = WakeUpCoral.copy(alpha = 0.14f).compositeOver(WakeUpSurface),
    onSecondaryContainer = WakeUpCoral,
    tertiary = WakeUpSage,
    onTertiary = Color.White,
    background = WakeUpBg,
    onBackground = WakeUpTextPrimary,
    surface = WakeUpSurface,
    onSurface = WakeUpTextPrimary,
    surfaceVariant = WakeUpSurfaceAlt,
    onSurfaceVariant = WakeUpTextSecondary,
    surfaceContainer = WakeUpSurface,
    surfaceContainerLow = WakeUpSurface,
    surfaceContainerHigh = WakeUpSurfaceAlt,
    surfaceContainerHighest = WakeUpSurfaceAlt,
    outline = WakeUpTextSecondary,
    outlineVariant = WakeUpBorder,
    error = WakeUpError,
)

// Variante oscura del mismo rediseño ("Night", ver Color.kt): mismos 6 acentos aclarados un poco
// para que no se vean apagados sobre fondo oscuro, sobre un carbón cálido (no negro puro). Mismo
// mapeo de roles que LightColors de arriba.
private val DarkColors = darkColorScheme(
    primary = WakeUpIndigoNight,
    onPrimary = WakeUpBgNight,
    primaryContainer = WakeUpIndigoNight.copy(alpha = 0.18f).compositeOver(WakeUpSurfaceNight),
    onPrimaryContainer = WakeUpIndigoNight,
    secondary = WakeUpCoralNight,
    onSecondary = WakeUpBgNight,
    secondaryContainer = WakeUpCoralNight.copy(alpha = 0.18f).compositeOver(WakeUpSurfaceNight),
    onSecondaryContainer = WakeUpCoralNight,
    tertiary = WakeUpSageNight,
    onTertiary = WakeUpBgNight,
    background = WakeUpBgNight,
    onBackground = WakeUpTextPrimaryNight,
    surface = WakeUpSurfaceNight,
    onSurface = WakeUpTextPrimaryNight,
    surfaceVariant = WakeUpSurfaceAltNight,
    onSurfaceVariant = WakeUpTextSecondaryNight,
    surfaceContainer = WakeUpSurfaceNight,
    surfaceContainerLow = WakeUpSurfaceNight,
    surfaceContainerHigh = WakeUpSurfaceAltNight,
    surfaceContainerHighest = WakeUpSurfaceAltNight,
    outline = WakeUpTextSecondaryNight,
    outlineVariant = WakeUpBorderNight,
    error = WakeUpErrorDark,
)

/**
 * Tema de la app: el rediseño 2026 ("Minimalismo táctil"), en su variante clara u oscura. Por
 * defecto sigue el tema del sistema ([isSystemInDarkTheme]) — quien llama a esto (ver [MainActivity])
 * puede pasar un [darkTheme] explícito para que la preferencia de Ajustes ("Claro"/"Oscuro"/
 * "Sistema") tenga la última palabra. El color dinámico (Android 12+) sigue disponible y
 * configurable desde Ajustes, en cuyo caso pisa esta paleta tanto en claro como en oscuro.
 */
@Composable
fun WakeUpTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> DarkColors
        else -> LightColors
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            window.navigationBarColor = colorScheme.background.toArgb()
            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = WakeUpTypography,
        shapes = WakeUpShapes,
        content = content,
    )
}
