package com.fritangui.wakeup.ui.theme

import androidx.compose.ui.graphics.Color

// Rediseño "Minimalismo táctil" (2026): fondo cálido neutro + acentos pastel-saturados para
// codificar prioridad/materia/estado. Reemplaza a la paleta azul/ámbar de abajo como identidad
// visual por defecto de la app (ver WakeUpTheme) — se mantiene *solo* en modo claro por ahora,
// los mockups del rediseño no cubrieron una variante oscura.
val WakeUpBg = Color(0xFFFAF7F2)
val WakeUpSurface = Color(0xFFFFFFFF)
val WakeUpSurfaceAlt = Color(0xFFF1EDE6)
val WakeUpBorder = Color(0xFFE6E1D7)
val WakeUpTextPrimary = Color(0xFF221F1B)
val WakeUpTextSecondary = Color(0xFF736C60)
val WakeUpTextTertiary = Color(0xFFA39C8E)

val WakeUpCoral = Color(0xFFE8735C)
val WakeUpAmber = Color(0xFFE3A548)
val WakeUpSage = Color(0xFF6FA786)
val WakeUpIndigo = Color(0xFF6B79D6)
val WakeUpLavender = Color(0xFF9C86C7)
val WakeUpSky = Color(0xFF5FA8C7)

// Radios de esquina y elevación del rediseño, en un solo lugar para no repetir los mismos números
// mágicos en cada pantalla (ver también Shape.kt, que expone los mismos radios como Shapes de M3).
val WakeUpShadowColor = Color(0xFF221F1B)

// --- Paleta anterior (2024): azules profundos + acento ámbar + verde-agua. Se conserva porque los
// widgets de home screen (Glance no puede leer MaterialTheme) siguen dibujándose con estos valores
// concretos — no son parte del rediseño de pantallas dentro de la app.
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
    Color(0xFF43A047), // verde bosque — antes era 0xFF26A69A, casi el mismo verde-agua que el de arriba
    Color(0xFFFF7043),
    Color(0xFF5C6BC0),
)

// Paleta más amplia para materias: suele haber más materias que carpetas dentro de un semestre,
// así que hacen falta más tonos para diferenciarlas de un vistazo (en la lista de tareas, los
// widgets, etc.). Empieza con los mismos 8 de FolderColorPalette (para que carpetas y materias
// compartan familia de colores) y suma 12 más — 20 en total, 2 filas completas en el selector.
val SubjectColorPalette = FolderColorPalette + listOf(
    Color(0xFFEC407A), // rosa
    Color(0xFF29B6F6), // celeste
    Color(0xFF8D6E63), // café
    Color(0xFF9CCC65), // verde lima
    Color(0xFF9E9D24), // oliva — antes era 0xFFFFCA28, casi el mismo amarillo que arriba
    Color(0xFF7E57C2), // violeta
    Color(0xFF26C6DA), // turquesa
    Color(0xFFFF8A65), // coral
    Color(0xFF607D8B), // gris azulado
    Color(0xFF283593), // azul marino
    Color(0xFFD81B60), // magenta
    Color(0xFF757575), // gris neutro
)
