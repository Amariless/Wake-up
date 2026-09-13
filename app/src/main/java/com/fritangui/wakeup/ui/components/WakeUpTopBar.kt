package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.Row

/**
 * Barra superior propia (#161): nada de "caja" — ni fondo ni alto fijo, solo el ícono de
 * navegación (si hay), el título y las acciones puestos directo sobre lo que sea que haya detrás
 * (el mismo fondo de la pantalla, sin ningún color propio que la distinga como un bloque aparte).
 * El alto sale SOLO del contenido: el ícono de 44dp cuando hay uno, si no, el propio texto del
 * título más un poco de aire — nunca un número fijo puesto a ojo.
 *
 * (Los dos intentos anteriores achicaban un alto fijo — 64dp → 56dp → 48dp — pero el usuario
 * reportó las dos veces que "seguía igual": el problema nunca fue CUÁNTO medía la caja, sino que
 * seguía siendo una caja con un alto impuesto en vez de uno que naciera del contenido real.)
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 4.dp, vertical = 6.dp),
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
