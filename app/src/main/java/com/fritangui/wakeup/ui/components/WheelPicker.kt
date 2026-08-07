package com.fritangui.wakeup.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.abs

/**
 * Selector tipo "rueda" (como el reloj nativo de Android): se desliza con el
 * dedo en cualquier dirección para subir o bajar el valor, en vez de tener
 * que tocar repetidamente un botón de "+1" que solo avanza.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WheelPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: androidx.compose.ui.unit.Dp = 40.dp,
    visibleCount: Int = 3,
    width: androidx.compose.ui.unit.Dp = 56.dp,
    label: (Int) -> String = { it.toString().padStart(2, '0') },
) {
    val items = remember(range) { range.toList() }
    val halfVisible = visibleCount / 2
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = (items.indexOf(value)).coerceAtLeast(0))
    val flingBehavior = rememberSnapFlingBehavior(listState)

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
                if (!scrolling) items.getOrNull(index)?.let { if (it != value) onValueChange(it) }
            }
    }

    // Si 'value' cambia desde afuera de la rueda (p.ej. al abrir el diálogo con un horario existente).
    LaunchedEffect(value) {
        val targetIndex = items.indexOf(value)
        if (targetIndex >= 0 && targetIndex != centerIndex && !listState.isScrollInProgress) {
            listState.scrollToItem(targetIndex)
        }
    }

    Box(modifier = modifier.width(width).height(itemHeight * visibleCount), contentAlignment = Alignment.Center) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = itemHeight * halfVisible),
            modifier = Modifier.fillMaxHeight().fillMaxWidth(),
        ) {
            itemsIndexed(items) { index, item ->
                val selected = index == centerIndex
                Box(modifier = Modifier.height(itemHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(
                        label(item),
                        style = if (selected) MaterialTheme.typography.headlineSmall else MaterialTheme.typography.bodyLarge,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                        color = if (selected) LocalContentColor.current else MaterialTheme.colorScheme.outline,
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
    androidx.compose.foundation.layout.Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        WheelPicker(value = hour, range = 0..23, onValueChange = onHourChange)
        Text(":", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.width(16.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        WheelPicker(value = minute, range = 0..59, onValueChange = onMinuteChange)
    }
}
