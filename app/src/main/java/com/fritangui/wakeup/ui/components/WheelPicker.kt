package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Selector tipo "rueda" (como el reloj nativo de Android/MIUI): se desliza con el dedo en
 * cualquier dirección para subir o bajar el valor, en vez de tener que tocar repetidamente un
 * botón de "+1". El número seleccionado se resalta (más grande, más opaco) y el efecto es
 * continuo: cada ítem visible calcula su propio "qué tan cerca está del centro" a partir de su
 * posición real de scroll (en píxeles) en cada frame, así que crece/decrece y aparece/se
 * desvanece de forma perfectamente fluida a medida que pasa por el centro — nunca "salta" de golpe
 * de un estilo a otro, ni mientras se desliza ni al asentarse, porque no hay ningún estado
 * discreto de por medio (nada de animateFloatAsState persiguiendo un valor "seleccionado sí/no").
 *
 * [loop] hace que el scroll dé la vuelta indefinidamente en cualquier dirección (arriba del
 * primer valor aparece el último, y viceversa) en vez de topar con un final — útil para horas
 * (0-23) y segundos/minutos (0-59) del temporizador, donde "no hay nada arriba del todo" se
 * sentía raro. Por dentro se logra repitiendo la lista de valores muchas veces y arrancando bien
 * en el medio de esa repetición, así hay margen de sobra para girar sin toparse con un final real.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 52.dp,
    visibleCount: Int = 5,
    width: Dp = 88.dp,
    loop: Boolean = false,
    label: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val items = remember(range) { range.toList() }
    val itemCount = items.size
    val loopRepeats = if (loop) 4000 else 1
    val virtualCount = itemCount * loopRepeats
    fun valueAt(virtualIndex: Int): Int = items[((virtualIndex % itemCount) + itemCount) % itemCount]

    val halfVisible = visibleCount / 2
    val initialIndex = remember(range) {
        if (loop) {
            val middleRepeat = (loopRepeats / 2) * itemCount
            (middleRepeat + items.indexOf(value).coerceAtLeast(0)).coerceIn(0, virtualCount - 1)
        } else {
            items.indexOf(value).coerceAtLeast(0)
        }
    }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val flingBehavior = rememberSnapFlingBehavior(listState)
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }

    val centerIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            if (info.visibleItemsInfo.isEmpty()) return@derivedStateOf listState.firstVisibleItemIndex
            val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs((it.offset + it.size / 2) - viewportCenter) }?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    // Reporta el valor solo cuando el scroll se asienta (evita disparar onValueChange en cada píxel).
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress to centerIndex }
            .distinctUntilChanged()
            .collect { (scrolling, index) ->
                if (!scrolling) {
                    val newValue = valueAt(index)
                    if (newValue != value) onValueChange(newValue)
                }
            }
    }

    // Si 'value' cambia desde afuera de la rueda (p.ej. al abrir el diálogo con un horario
    // existente): busca el índice virtual más cercano al actual que ya represente ese valor, en
    // vez de siempre saltar hacia adelante — así el salto visual es el más corto posible.
    LaunchedEffect(value) {
        if (listState.isScrollInProgress) return@LaunchedEffect
        if (valueAt(centerIndex) == value) return@LaunchedEffect
        val targetOffsetInCycle = items.indexOf(value)
        if (targetOffsetInCycle < 0) return@LaunchedEffect
        val currentOffsetInCycle = ((centerIndex % itemCount) + itemCount) % itemCount
        val forward = (targetOffsetInCycle - currentOffsetInCycle + itemCount) % itemCount
        val backward = forward - itemCount
        val steps = if (abs(forward) <= abs(backward)) forward else backward
        val target = (centerIndex + steps).coerceIn(0, virtualCount - 1)
        listState.scrollToItem(target)
    }

    val dimColor = MaterialTheme.colorScheme.outline
    val emphasisColor = LocalContentColor.current

    Box(modifier = modifier.width(width).height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        ) {
            items(count = virtualCount, key = { it }) { index ->
                // 't' va de 1 (justo en el centro) a 0 (a halfVisible ítems de distancia o más),
                // calculado a partir de la posición real de layout de ESTE ítem en cada frame — por
                // eso no hace falta ninguna animación aparte, el propio scroll ya es la animación.
                val info = listState.layoutInfo
                val viewportCenter = (info.viewportStartOffset + info.viewportEndOffset) / 2f
                val itemInfo = info.visibleItemsInfo.firstOrNull { it.index == index }
                val centerOffsetPx = itemInfo?.let { (it.offset + it.size / 2f) - viewportCenter }
                val distanceInItems = if (centerOffsetPx != null) abs(centerOffsetPx) / itemHeightPx else halfVisible.toFloat()
                val t = (1f - (distanceInItems / halfVisible.toFloat())).coerceIn(0f, 1f)
                val scale = 0.68f + 0.32f * t
                val color = lerp(dimColor, emphasisColor, t)

                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(valueAt(index)),
                        style = MaterialTheme.typography.headlineMedium,
                        color = color,
                        modifier = Modifier.graphicsLayer { scaleX = scale; scaleY = scale },
                    )
                }
            }
        }
        HorizontalDivider(modifier = Modifier.align(Alignment.Center).width(width))
    }
}

/** Wheel picker de hora:minuto, con dos ruedas lado a lado. */
@Composable
fun WheelTimePicker(
    hour: Int,
    minute: Int,
    onHourChange: (Int) -> Unit,
    onMinuteChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(value = hour, range = 0..23, onValueChange = onHourChange)
        Text(":", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.width(20.dp), textAlign = TextAlign.Center)
        WheelPicker(value = minute, range = 0..59, onValueChange = onMinuteChange)
    }
}
