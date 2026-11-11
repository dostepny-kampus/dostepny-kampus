/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val ICON_GRID = 24f

/**
 * Ikona z jednej ścieżki [path] (dane `pathData` z Material Icons) na siatce 24 × 24, wypełniona czernią; kolor
 * nadaje dopiero `Icon`. [name] to nazwa wektora, a [autoMirror] odbija ikonę w układzie od prawej do lewej
 * (strzałki kierunkowe). Jedyne miejsce w aplikacji, które buduje `ImageVector` ze ścieżki: ekrany i moduły
 * funkcji wołają tę funkcję i nie budują wektorów same.
 */
fun pathIcon(path: String, name: String = "", autoMirror: Boolean = false): ImageVector {
    val builder =
        ImageVector.Builder(
            name = name,
            defaultWidth = ICON_GRID.dp,
            defaultHeight = ICON_GRID.dp,
            viewportWidth = ICON_GRID,
            viewportHeight = ICON_GRID,
            autoMirror = autoMirror,
        )
    builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
    return builder.build()
}
