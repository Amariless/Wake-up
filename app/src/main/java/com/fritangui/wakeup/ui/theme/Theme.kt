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

// Paleta oscura hecha a mano (no solo darkColorScheme(primary=...)): controla también los
// "container" y las superficies escalonadas que usan Card/NavigationBar/Slider/Chips, para que
// no se sientan todas del mismo gris genérico de Material.
private val DarkColors = darkColorScheme(
    primary = WakeUpPrimaryDark,
    onPrimary = WakeUpOnPrimaryDark,
    primaryContainer = WakeUpPrimaryContainerDark,
    onPrimaryContainer = WakeUpOnPrimaryContainerDark,
    secondary = WakeUpSecondary,
    onSecondary = WakeUpOnSecondaryDark,
    secondaryContainer = WakeUpSecondaryContainerDark,
    onSecondaryContainer = WakeUpOnSecondaryContainerDark,
    tertiary = WakeUpTertiary,
    onTertiary = WakeUpOnTertiaryDark,
    background = WakeUpBackgroundDark,
    onBackground = androidx.compose.ui.graphics.Color(0xFFE3E5EC),
    surface = WakeUpSurfaceDark,
    onSurface = androidx.compose.ui.graphics.Color(0xFFE3E5EC),
    surfaceVariant = WakeUpSurfaceVariantDark,
    onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFC4CAD9),
    surfaceContainer = WakeUpSurfaceContainerDark,
    surfaceContainerHigh = WakeUpSurfaceContainerHighDark,
    surfaceContainerHighest = WakeUpSurfaceVariantDark,
    outline = WakeUpOutlineDark,
    outlineVariant = WakeUpOutlineVariantDark,
    error = WakeUpErrorDark,
)

/**
 * Tema de la app. El default es **claro siempre** (no sigue el tema del sistema): es la identidad
 * visual del rediseño 2026 ("Minimalismo táctil"), pensada como fondo cálido neutro de punta a
 * punta. `DarkColors` se conserva por si se vuelve a exponer un modo oscuro más adelante, pero hoy
 * nada pasa `darkTheme = true`. El color dinámico (Android 12+) sigue disponible y configurable
 * desde Ajustes.
 */
@Composable
fun WakeUpTheme(
    darkTheme: Boolean = false,
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
