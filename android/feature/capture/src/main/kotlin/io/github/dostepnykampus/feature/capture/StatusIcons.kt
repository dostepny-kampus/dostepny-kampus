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

    /** „W weryfikacji”: oko. */
    val InReview: ImageVector =
        icon(
            "M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17" +
                "c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3" +
                "-1.34-3-3-3z",
        )

    /** „Przyjęte”: znak bez kółka. */
    val Accepted: ImageVector = icon("M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z")

    /** „Odrzucone”: krzyżyk w kółku. */
    val Rejected: ImageVector =
        icon(
            "M12 2C6.47 2 2 6.47 2 12s4.47 10 10 10 10-4.47 10-10S17.53 2 12 2zm5 13.59L15.59 17 12 13.41 8.41 17" +
                " 7 15.59 10.59 12 7 8.41 8.41 7 12 10.59 15.59 7 17 8.41 13.41 12 17 15.59z",
        )

    /** „Archiwalne”: pudełko archiwum. */
    val Archived: ImageVector =
        icon(
            "M20.54 5.23l-1.39-1.68C18.88 3.21 18.47 3 18 3H6c-.47 0-.88.21-1.16.55L3.46 5.23C3.17 5.57 3 6.02 3 6.5" +
                "V19c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2V6.5c0-.48-.17-.93-.46-1.27z" +
                "M12 17.5L6.5 12H10v-2h4v2h3.5L12 17.5zM5.12 5l.81-1h12l.94 1H5.12z",
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
