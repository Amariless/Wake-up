package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
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
 * texto — "todas comparten la misma caja", como lo describió el usuario, así que el arreglo va acá
 * en un solo lugar en vez de screen por screen. 56dp es la altura "estándar" pre-M3 (Material 2),
 * más compacta pero sin quedar apretada para los IconButton de 48dp que casi todas las pantallas
 * ponen a los costados.
 *
 * Mismos parámetros con los mismos nombres que [androidx.compose.material3.TopAppBar] a propósito,
 * para que reemplazarla en cada pantalla sea solo cambiar el nombre de la función.
 */
@Composable
fun WakeUpTopBar(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    Surface(color = MaterialTheme.colorScheme.surface, modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .height(56.dp)
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) { navigationIcon() }
            Box(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                ProvideTextStyle(MaterialTheme.typography.titleLarge) { title() }
            }
            Row(verticalAlignment = Alignment.CenterVertically, content = actions)
        }
    }
}
