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

private const val VIEWPORT = 24f

/**
 * Ikony pozycji paska dolnego ([BottomBarItem]) według słownika użytkownika (sekcja „Metafory i ikony”): ścieżki
 * obrysowane z Material Icons, siatka 24 × 24. Ikona stoi zawsze nad napisem, który sam niesie znaczenie (NFR-21).
 */
object NavigationIcons {
    /** „Mapa”: złożony plan, jak w aplikacjach map. */
    val Map: ImageVector =
        icon(
            name = "Map",
            path =
                "M20.5 3l-.16.03L15 5.1 9 3 3.36 4.9c-.21.07-.36.25-.36.48V20.5c0 .28.22.5.5.5l.16-.03L9 18.9l6 " +
                    "2.1 5.64-1.9c.21-.07.36-.25.36-.48V3.5c0-.28-.22-.5-.5-.5zM10 5.47l4 1.4v11.66l-4-1.4V5.47zm-5 " +
                    ".99l3-1.01v11.7l-3 1.16V6.46zm14 11.08l-3 1.01V6.86l3-1.16v11.84z",
        )

    /** „Zgłoś barierę”: aparat, bo zgłoszenie zaczyna się od zdjęcia (US4.1). */
    val Camera: ImageVector =
        icon(
            name = "Camera",
            path =
                "M14.12 4l1.83 2H20v12H4V6h4.05l1.83-2h4.24M15 2H9L7.17 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16" +
                    "c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2h-3.17L15 2zm-3 7c1.65 0 3 1.35 3 3s-1.35 3-3 3-3-1.35-3-3 " +
                    "1.35-3 3-3m0-2c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5z",
        )

    /** „Pomoc”: znak zapytania. */
    val Help: ImageVector =
        icon(
            name = "Help",
            path =
                "M11.07 12.85c.77-1.39 2.25-2.21 3.11-3.44.91-1.29.4-3.7-2.18-3.7-1.69 0-2.52 1.28-2.87 2.34L6.54 " +
                    "6.96C7.25 4.83 9.18 3 11.99 3c2.35 0 3.96 1.07 4.78 2.41.7 1.15 1.11 3.3.03 4.9-1.2 1.77-2.35 " +
                    "2.31-2.97 3.45-.25.46-.35.76-.35 2.24h-2.89c-.01-.78-.13-2.05.48-3.15zM14 20c0 1.1-.9 2-2 2" +
                    "s-2-.9-2-2 .9-2 2-2 2 .9 2 2z",
        )

    /** „Konto” i „Konto i praca”: sylwetka, ta sama dla obu napisów. */
    val Account: ImageVector =
        icon(
            name = "Account",
            path =
                "M12 5.9c1.16 0 2.1.94 2.1 2.1s-.94 2.1-2.1 2.1S9.9 9.16 9.9 8s.94-2.1 2.1-2.1m0 9c2.97 0 6.1 " +
                    "1.46 6.1 2.1v1.1H5.9V17c0-.64 3.13-2.1 6.1-2.1M12 4C9.79 4 8 5.79 8 8s1.79 4 4 4 4-1.79 " +
                    "4-4-1.79-4-4-4zm0 9c-2.67 0-8 1.34-8 4v3h16v-3c0-2.66-5.33-4-8-4z",
        )

    private fun icon(name: String, path: String): ImageVector {
        val builder =
            ImageVector.Builder(
                name = name,
                defaultWidth = VIEWPORT.dp,
                defaultHeight = VIEWPORT.dp,
                viewportWidth = VIEWPORT,
                viewportHeight = VIEWPORT,
            )
        builder.addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        return builder.build()
    }
}
