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

    /** Informacja o danych osobowych. */
    val PrivacyNotice: ImageVector =
        pathIcon(
            "M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2" +
                "zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z",
            name = "Description",
        )

    /** Wycofanie zgody. */
    val WithdrawConsent: ImageVector =
        pathIcon(
            "M12 2L4 5v6.09c0 5.05 3.41 9.76 8 10.91 4.59-1.15 8-5.86 8-10.91V5l-8-3zm3.5 12.09-1.41 1.41" +
                "L12 13.41l-2.09 2.09-1.41-1.41L10.59 12 8.5 9.91 9.91 8.5 12 10.59l2.09-2.09 1.41 1.41" +
                "L13.41 12l2.09 2.09z",
            name = "GppBad",
        )

    /** Odnośnik zewnętrzny otwierany w przeglądarce. */
    val ExternalLink: ImageVector =
        pathIcon(
            "M19 19H5V5h7V3H5c-1.11 0-2 .9-2 2v14c0 1.1.89 2 2 2h14c1.1 0 2-.9 2-2v-7h-2v7zM14 3v2h3.59" +
                "l-9.83 9.83 1.41 1.41L19 6.41V10h2V3h-7z",
            name = "OpenInNew",
        )

    /** Wylogowanie. */
    val SignOut: ImageVector =
        pathIcon(
            path =
                "M17 7l-1.41 1.41L18.17 11H8v2h10.17l-2.58 2.58L17 17l5-5zM4 5h8V3H4c-1.1 0-2 .9-2 2v14" +
                    "c0 1.1.9 2 2 2h8v-2H4V5z",
            name = "Logout",
            autoMirror = true,
        )

    private val Measure: ImageVector =
        pathIcon(
            path =
                "M21 6H3c-1.1 0-2 .9-2 2v8c0 1.1.9 2 2 2h18c1.1 0 2-.9 2-2V8c0-1.1-.9-2-2-2zm0 10H3V8h2v4h2V8" +
                    "h2v4h2V8h2v4h2V8h2v4h2V8h2v8z",
            name = "Straighten",
        )

    private val Mine: ImageVector =
        pathIcon(
            path =
                "M4 10.5c-.83 0-1.5.67-1.5 1.5s.67 1.5 1.5 1.5 1.5-.67 1.5-1.5-.67-1.5-1.5-1.5zm0-6" +
                    "c-.83 0-1.5.67-1.5 1.5S3.17 7.5 4 7.5 5.5 6.83 5.5 6 4.83 4.5 4 4.5zm0 12c-.83 0-1.5.68-1.5 1.5" +
                    "s.68 1.5 1.5 1.5 1.5-.68 1.5-1.5-.67-1.5-1.5-1.5zM7 19h14v-2H7v2zm0-6h14v-2H7v2zm0-8v2h14V5H7z",
            name = "FormatListBulleted",
        )

    private val Progress: ImageVector =
        pathIcon(path = "M4 9h4v11H4zm12 4h4v7h-4zm-6-9h4v16h-4z", name = "BarChart")

    private val FactCheck: ImageVector =
        pathIcon(
            path =
                "M20 3H4c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V5c0-1.1-.9-2-2-2zM4 19V5h16" +
                    "v14H4zm2-9 2 2 4-4-1.4-1.4L8 9.2l-.6-.6L6 10zm0 6 2 2 4-4-1.4-1.4L8 15.2l-.6-.6L6 16zm" +
                    "7-7h5v2h-5zm0 6h5v2h-5z",
            name = "FactCheck",
        )

    private val PersonAdd: ImageVector =
        pathIcon(
            path =
                "M15 12c2.21 0 4-1.79 4-4s-1.79-4-4-4-4 1.79-4 4 1.79 4 4 4zm-7-2V7H6v3H3v2h3v3h2v-3h3v" +
                    "-2H8zm7 4c-2.67 0-8 1.34-8 4v2h16v-2c0-2.66-5.33-4-8-4z",
            name = "PersonAdd",
        )

    private val Download: ImageVector =
        pathIcon(path = "M5 20h14v-2H5v2zm7-4 5-5h-3V4h-4v7H7l5 5z", name = "Download")

    /** Ikona czynności [work]. */
    fun iconOf(work: WorkAction): ImageVector =
        when (work) {
            WorkAction.MEASURE -> Measure
            WorkAction.MINE -> Mine
            WorkAction.PROGRESS -> Progress
            WorkAction.REVIEW -> FactCheck
            WorkAction.INVITE -> PersonAdd
            WorkAction.EXPORT -> Download
        }
}
