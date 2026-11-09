/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private const val VIEWPORT = 24f

/** Ikony statusów C3; tylko wzmacniają słowo statusu (NFR-21). */
internal object StatusIcons {
    /** „W telefonie”: telefon. */
    val Phone: ImageVector =
        icon(
            "M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z",
        )

    /** „Wysłane”: znak w kółku. */
    val Sent: ImageVector =
        icon(
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17" +
                "l7.59-7.59L19 8l-9 9z",
        )

    /** „Nie wysłano”: wykrzyknik w kółku. */
    val Refused: ImageVector =
        icon(
            "M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z",
        )

    /** „Otwórz”: strzałka w prawo jak w R1 i K1. */
    val Open: ImageVector = icon("M12 4l-1.41 1.41L16.17 11H4v2h12.17l-5.58 5.59L12 20l8-8z")

    private fun icon(path: String): ImageVector {
        val builder =
            ImageVector.Builder(
                defaultWidth = VIEWPORT.dp,
                defaultHeight = VIEWPORT.dp,
                viewportWidth = VIEWPORT,
                viewportHeight = VIEWPORT,
            )
        builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        return builder.build()
    }
}
