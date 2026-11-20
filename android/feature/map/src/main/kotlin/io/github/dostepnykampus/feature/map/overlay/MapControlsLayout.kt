/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.overlay

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasureScope
import io.github.dostepnykampus.core.designsystem.Odstepy

/**
 * Układa przełącznik terenu, piętra i sterowanie mapą we wspólnym obszarze. Dół zostawia na podpis i legendę.
 *
 * Wymagania: NFR-20, NFR-53, US1.7, US16.1
 */
@Composable
internal fun MapControlsLayout(
    mapControls: @Composable () -> Unit,
    areaSwitch: @Composable () -> Unit,
    floorColumn: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Layout(
        contents = listOf(mapControls, areaSwitch, floorColumn),
        modifier = modifier,
    ) { (buttons, area, floors), bounds ->
        val loose = bounds.copy(minWidth = 0, minHeight = 0)
        val footer = (MapControlTouchSize + Odstepy.s).roundToPx()
        val contentHeight = (loose.maxHeight - footer).coerceAtLeast(0)
        val switch = area.singleOrNull()?.measure(loose.copy(maxHeight = contentHeight))
        val floorsTop = switch?.height ?: 0
        val floorsHeight = (contentHeight - floorsTop).coerceAtLeast(0)
        val floorPicker = floors.singleOrNull()?.measure(loose.copy(maxHeight = floorsHeight))
        val controls = buttons.singleOrNull()?.measure(loose)
        layout(width = bounds.maxWidth, height = bounds.maxHeight) {
            if (controls != null) {
                val start = bounds.maxWidth - controls.width
                val endOfSwitch = (switch?.width ?: 0) - Odstepy.s.roundToPx()
                val top = controlsTop(start = start, switchEnd = endOfSwitch, switchBottom = floorsTop)
                controls.placeRelative(x = start, y = top)
            }
            switch?.placeRelative(x = 0, y = 0)
            floorPicker?.placeRelative(x = 0, y = floorsTop)
        }
    }
}

private fun MeasureScope.controlsTop(start: Int, switchEnd: Int, switchBottom: Int): Int {
    val inset = Odstepy.s.roundToPx()
    val cell = MapControlTouchSize.roundToPx()
    val emptyCell = if (switchBottom <= inset + cell) cell else 0
    return if (switchEnd <= start + inset + emptyCell) 0 else switchBottom
}
