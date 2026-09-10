package com.fritangui.wakeup.ui.theme

import androidx.compose.ui.graphics.Color

// Rediseño "Minimalismo táctil" (2026): fondo cálido neutro + acentos pastel-saturados para
// codificar prioridad/materia/estado. Reemplaza a la paleta azul/ámbar de abajo como identidad
// visual por defecto de la app (ver WakeUpTheme).
val WakeUpBg = Color(0xFFFAF7F2)
val WakeUpSurface = Color(0xFFFFFFFF)
val WakeUpSurfaceAlt = Color(0xFFF1EDE6)
val WakeUpBorder = Color(0xFFE6E1D7)
val WakeUpTextPrimary = Color(0xFF221F1B)
val WakeUpTextSecondary = Color(0xFF736C60)
val WakeUpTextTertiary = Color(0xFFA39C8E)

// Coral y salvia bajaron un pelín de luminosidad (imperceptible a simple vista) respecto a los
// valores originales del mockup: como íconos/texto sobre superficie clara (nav inferior
// seleccionado, acciones rápidas, tarjeta de Bienestar) medían 2.6-3.0:1 de contraste — por debajo
// del mínimo 3:1 de WCAG 2.2 para gráficos/texto grande (1.4.11). Ahora dan ~3.0-3.1:1. Sin cambios
// en modo oscuro (WakeUpCoralNight/WakeUpSageNight ya cumplían de sobra, 6.9-8.4:1).
val WakeUpCoral = Color(0xFFE76F58)
val WakeUpAmber = Color(0xFFE3A548)
val WakeUpSage = Color(0xFF66A17E)
val WakeUpIndigo = Color(0xFF6B79D6)
val WakeUpLavender = Color(0xFF9C86C7)
val WakeUpSky = Color(0xFF5FA8C7)

// Variante oscura del mismo sistema (sufijo "Night" para no confundirla ni con la paleta clara de
// arriba ni con los "*Dark" de abajo, que son de los widgets y no tienen nada que ver con esto):
// mismos 6 acentos, pero un poco más claros/luminosos — si se dejaran igual de saturados que en
// fondo claro se verían apagados sobre un fondo oscuro. Los neutros son un carbón cálido, no negro
// puro, para no perder la sensación "táctil" del rediseño.
val WakeUpBgNight = Color(0xFF1C1A17)
val WakeUpSurfaceNight = Color(0xFF262320)
val WakeUpSurfaceAltNight = Color(0xFF302D29)
val WakeUpBorderNight = Color(0xFF3D3934)
val WakeUpTextPrimaryNight = Color(0xFFF5F1EA)
val WakeUpTextSecondaryNight = Color(0xFFB6AEA1)
val WakeUpTextTertiaryNight = Color(0xFF7D766A)

val WakeUpCoralNight = Color(0xFFF0876F)
val WakeUpAmberNight = Color(0xFFEDBB6C)
val WakeUpSageNight = Color(0xFF86C29B)
val WakeUpIndigoNight = Color(0xFF8B97E0)
val WakeUpLavenderNight = Color(0xFFB7A0D9)
val WakeUpSkyNight = Color(0xFF79BEDA)

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
