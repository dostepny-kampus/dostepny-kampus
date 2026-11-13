/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Linia „Bez internetu” na V1 z dniem pobrania danych z serwera. Wymagania: US9.1, NFR-10 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class OfflineLineTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: dane pobrane 27.10.2026 przed północą czasu Wrocławia → „dane z 27.10.2026”. Wymagania: NFR-10 */
    @Test
    fun nfr10_line_has_download_day() {
        compose.setContent {
            DostepnyKampusTheme { OfflineLine(syncedAt = Instant.parse("2026-10-27T22:30:00Z")) }
        }
        compose.onNodeWithText("Bez internetu · dane z 27.10.2026").assertIsDisplayed()
    }
}
