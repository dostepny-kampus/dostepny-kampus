/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.dp

private val WideWindow = 600.dp
private val ReadingColumn = 560.dp

/**
 * Ogranicza mierzoną treść do 560 dp przy dostępnej szerokości od 600 dp i umieszcza ją pośrodku. W węższym albo
 * nieograniczonym oknie zachowuje jego ograniczenia. Wstaw przed modyfikatorami wypełniającymi szerokość.
 *
 * Wymagania: NFR-51
 */
public fun Modifier.readableWidth(): Modifier = layout { measurable, constraints ->
    val column =
        if (constraints.hasBoundedWidth && constraints.maxWidth >= WideWindow.roundToPx()) {
            ReadingColumn.roundToPx()
        } else {
            constraints.maxWidth
        }
    val placeable =
        measurable.measure(constraints.copy(minWidth = constraints.minWidth.coerceAtMost(column), maxWidth = column))
    val width = if (constraints.hasBoundedWidth) constraints.maxWidth else placeable.width
    layout(width = width, height = placeable.height) {
        placeable.placeRelative(x = (width - placeable.width) / 2, y = 0)
    }
}
