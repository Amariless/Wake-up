package com.fritangui.wakeup.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta base "Wake up": azules profundos + acento ámbar (alarma/energía) + verde-agua (bienestar)
val WakeUpPrimary = Color(0xFF3D5AFE)
val WakeUpPrimaryDark = Color(0xFF8DA0FF)
val WakeUpOnPrimaryDark = Color(0xFF0B1130)
val WakeUpPrimaryContainerDark = Color(0xFF2A3670)
val WakeUpOnPrimaryContainerDark = Color(0xFFDDE1FF)

val WakeUpSecondary = Color(0xFFFFC857)
val WakeUpOnSecondaryDark = Color(0xFF3F2E00)
val WakeUpSecondaryContainerDark = Color(0xFF5B4300)
val WakeUpOnSecondaryContainerDark = Color(0xFFFFE29B)

val WakeUpTertiary = Color(0xFF4DD0C4)
val WakeUpOnTertiaryDark = Color(0xFF00382F)

val WakeUpBackgroundLight = Color(0xFFFAFAFE)
val WakeUpBackgroundDark = Color(0xFF0B0E14)
val WakeUpSurfaceLight = Color(0xFFFFFFFF)
val WakeUpSurfaceDark = Color(0xFF151A24)
val WakeUpSurfaceVariantDark = Color(0xFF232B3B)
val WakeUpSurfaceContainerDark = Color(0xFF1B2130)
val WakeUpSurfaceContainerHighDark = Color(0xFF262E40)
val WakeUpOutlineDark = Color(0xFF8B93A7)
val WakeUpOutlineVariantDark = Color(0xFF3A4356)

val WakeUpError = Color(0xFFBA1A1A)
val WakeUpErrorDark = Color(0xFFFFB4AB)
val WakeUpOnPrimary = Color(0xFFFFFFFF)
val WakeUpOnSecondary = Color(0xFF1B2430)

// Colores de "carpeta"/semestre, asignables por el usuario
val FolderColorPalette = listOf(
    Color(0xFF3D5AFE),
    Color(0xFF00BFA6),
    Color(0xFFFFC857),
    Color(0xFFEF5350),
    Color(0xFFAB47BC),
    Color(0xFF26A69A),
    Color(0xFFFF7043),
    Color(0xFF5C6BC0),
)

// Paleta más amplia para materias: suele haber más materias que carpetas dentro de un semestre,
// así que hacen falta más tonos para diferenciarlas de un vistazo (en la lista de tareas, los
// widgets, etc.). Empieza con los mismos 8 de FolderColorPalette (para que carpetas y materias
// compartan familia de colores) y suma 8 más.
val SubjectColorPalette = FolderColorPalette + listOf(
    Color(0xFFEC407A), // rosa
    Color(0xFF29B6F6), // celeste
    Color(0xFF8D6E63), // café
    Color(0xFF9CCC65), // verde lima
    Color(0xFFFFCA28), // ámbar
    Color(0xFF7E57C2), // violeta
    Color(0xFF26C6DA), // turquesa
    Color(0xFFFF8A65), // coral
)
