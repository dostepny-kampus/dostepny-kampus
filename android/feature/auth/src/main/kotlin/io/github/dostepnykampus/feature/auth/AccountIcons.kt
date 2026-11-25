/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.auth

import androidx.compose.ui.graphics.vector.ImageVector
import io.github.dostepnykampus.core.designsystem.pathIcon

/** Ikony wierszy L2: ścieżki z Material Icons na siatce 24 × 24. Wymagania: NFR-21 */
internal object AccountIcons {
    /** Przejście dalej, do kolejnego widoku albo kroku. */
    val Forward: ImageVector =
        pathIcon(path = "M10 6L8.59 7.41 13.17 12l-4.58 4.59L10 18l6-6z", name = "ChevronRight", autoMirror = true)

    /** Wylogowanie. */
    val SignOut: ImageVector =
        pathIcon(
            path = "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14" +
                "c0 1.1.9 2 2 2h8v-2H4V5z",
            name = "Logout",
            autoMirror = true,
        )

    private val Measure: ImageVector =
        pathIcon(
            path = "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h2v4h2V8" +
                "h2v4h2V8h2v4h2V8h2v4h2V8h2v8z",
            name = "Straighten",
        )

    private val Mine: ImageVector =
        pathIcon(
            path = "M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6" +
                "c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.68-1.5 1.5" +
                "s.68 1.5 1.5 1.5 1.5-.68 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z",
            name = "FormatListBulleted",
            autoMirror = true,
        )

    private val Progress: ImageVector =
        pathIcon(path = "M4 9h4v11H4zm12 4h4v7h-4zm-6-9h4v16h-4z", name = "BarChart")

    /** Ikona czynności [work]. */
    fun of(work: WorkAction): ImageVector =
        when (work) {
            WorkAction.MEASURE -> Measure
            WorkAction.MINE -> Mine
            WorkAction.PROGRESS -> Progress
        }
}
