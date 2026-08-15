package com.fritangui.wakeup.ui.subjects

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Agriculture
import androidx.compose.material.icons.filled.Architecture
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Balance
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Coronavirus
import androidx.compose.material.icons.filled.DesignServices
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Engineering
import androidx.compose.material.icons.filled.Factory
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.SportsBasketball
import androidx.compose.material.icons.filled.SportsSoccer
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material.icons.filled.TheaterComedy
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.filled.Water
import androidx.compose.material.icons.outlined.School
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Catálogo fijo de íconos elegibles para una materia (#reconocerlas más fácil de un vistazo).
 * A propósito hay muchos más de los estrictamente necesarios ("mejor que sobre a que falte", pedido
 * del usuario) para cubrir la variedad real de carreras/materias, no solo ingeniería.
 */
object SubjectIcons {
    val catalog: List<Pair<String, ImageVector>> = listOf(
        "school" to Icons.Outlined.School,
        "calculate" to Icons.Default.Calculate,
        "functions" to Icons.Default.Functions,
        "science" to Icons.Default.Science,
        "biotech" to Icons.Default.Biotech,
        "coronavirus" to Icons.Default.Coronavirus,
        "code" to Icons.Default.Code,
        "cloud" to Icons.Default.Cloud,
        "engineering" to Icons.Default.Engineering,
        "architecture" to Icons.Default.Architecture,
        "factory" to Icons.Default.Factory,
        "design" to Icons.Default.DesignServices,
        "menu_book" to Icons.Default.MenuBook,
        "article" to Icons.Default.Article,
        "history" to Icons.Default.History,
        "public" to Icons.Default.Public,
        "language" to Icons.Default.Language,
        "translate" to Icons.Default.Translate,
        "psychology" to Icons.Default.Psychology,
        "groups" to Icons.Default.Groups,
        "gavel" to Icons.Default.Gavel,
        "balance" to Icons.Default.Balance,
        "business" to Icons.Default.BusinessCenter,
        "money" to Icons.Default.AttachMoney,
        "health" to Icons.Default.HealthAndSafety,
        "hospital" to Icons.Default.LocalHospital,
        "agriculture" to Icons.Default.Agriculture,
        "pets" to Icons.Default.Pets,
        "park" to Icons.Default.Park,
        "landscape" to Icons.Default.Landscape,
        "water" to Icons.Default.Water,
        "flight" to Icons.Default.Flight,
        "car" to Icons.Default.DirectionsCar,
        "brush" to Icons.Default.Brush,
        "music" to Icons.Default.MusicNote,
        "piano" to Icons.Default.Piano,
        "theater" to Icons.Default.TheaterComedy,
        "movie" to Icons.Default.Theaters,
        "camera" to Icons.Default.CameraAlt,
        "restaurant" to Icons.Default.Restaurant,
        "soccer" to Icons.Default.SportsSoccer,
        "basketball" to Icons.Default.SportsBasketball,
    )

    fun iconFor(key: String?): ImageVector? = key?.let { k -> catalog.firstOrNull { it.first == k }?.second }
}
