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
 * Centers a reading column in windows from 600 dp. The 560 dp column leaves room for the usual
 * 16 dp padding and keeps bodyLarge prose below 75 characters per typical line (NFR-51).
 * Fields and actions receive the same constraints, including at 200% font size. Apply before
 * fillMaxWidth/fillMaxSize; maps and plans keep their full available width.
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
