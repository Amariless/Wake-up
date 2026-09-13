package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row

/**
 * Barra superior propia (#161), más baja que la [androidx.compose.material3.TopAppBar] estándar
 * de Material3: esa siempre mide 64dp de alto sin ningún parámetro para achicarla, y con títulos
 * de una sola línea (casi todas las pantallas) se sentía con mucho aire vacío arriba y abajo del
 * texto — "todas comparten la misma caja", como lo describió el usuario. Primer intento: 56dp —
 * el usuario reportó que "seguía igual", así que este pase es más decisivo: 48dp (el mínimo touch
 * target recomendado, no hay forma razonable de ir más chico) y sin reservar espacio para un ícono
 * de navegación que casi ninguna pantalla de nivel superior (Inicio, Carpetas, Reloj, Bienestar)
 * tiene — antes esas pantallas igual arrancaban con una caja vacía de 48dp a la izquierda.
 *
 * Mismos parámetros con los mismos nombres que [androidx.compose.material3.TopAppBar] a propósito,
 * para que reemplazarla en cada pantalla sea solo cambiar el nombre de la función.
 */
@Composable
fun WakeUpTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(48.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (navigationIcon != null) {
                Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) { navigationIcon() }
            } else {
                Spacer(modifier = Modifier.width(12.dp))
            }
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleMedium) { title() }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
