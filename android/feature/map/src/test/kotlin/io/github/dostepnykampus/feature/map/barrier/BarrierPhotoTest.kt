/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.barrier

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import coil3.intercept.Interceptor
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.feature.map.PHOTO_URL
import io.github.dostepnykampus.feature.map.photoAvailable
import io.github.dostepnykampus.feature.map.photoFailing
import io.github.dostepnykampus.feature.map.photoPending
import io.github.dostepnykampus.feature.map.usePhotoLoader
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Zdjęcie zgłoszenia w arkuszu bariery. Wymagania: US1.5, BR-17, NFR-12, NFR-18, NFR-50 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BarrierPhotoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: bariera z adresem zdjęcia → w arkuszu obraz z opisem „Zdjęcie bariery: typ”. Wymagania: NFR-18 */
    @Test
    fun showsPhotoWithDescription() {
        show(PHOTO_URL, photoAvailable())

        compose.waitUntil(WAIT_MS) { shown(DESCRIPTION) }
        compose.onNodeWithContentDescription(DESCRIPTION).assertIsDisplayed()
    }

    /** Wiersz: bariera bez zdjęcia → w arkuszu nie ma obrazu ani napisu o wczytywaniu. Wymagania: BR-17 */
    @Test
    fun hidesPhotoWithoutAddress() {
        show(null, photoAvailable())

        compose.waitForIdle()
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
        compose.onNodeWithText("Wczytujemy zdjęcie…").assertDoesNotExist()
    }

    /** Wiersz: zdjęcie się wczytuje → w jego miejscu napis „Wczytujemy zdjęcie…”. Wymagania: NFR-12 */
    @Test
    fun showsLoadingNoteWhilePending() {
        show(PHOTO_URL, photoPending())

        compose.onNodeWithText("Wczytujemy zdjęcie…").assertIsDisplayed()
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
    }

    /** Wiersz: błąd pobrania → komunikat i „Spróbuj ponownie” o wysokości co najmniej 48 dp. Wymagania: NFR-50 */
    @Test
    fun showsErrorWithRetryOnFailure() {
        show(PHOTO_URL, photoFailing())

        val message = "Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę."
        compose.waitUntil(WAIT_MS) { shownText(message) }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
    }

    private fun shown(description: String) =
        compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    private fun shownText(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    private fun show(url: String?, loader: Interceptor) {
        usePhotoLoader(compose.activity, loader)
        compose.setContent {
            DostepnyKampusTheme { BarrierPhoto(ChosenBarrier(barrier(url), building = null, entrance = null)) }
        }
    }

    private fun barrier(url: String?) =
        AcceptedBarrier(
            id = "z1",
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            severity = Severity.BLOCKING,
            type = BarrierType.STAIRS_AT_ENTRANCE,
            buildingSymbol = null,
            verifiedAt = null,
            photoIds = listOfNotNull(url?.let { "p1" }),
            photoUrl = url,
        )

    private companion object {
        const val WAIT_MS = 5_000L
        const val DESCRIPTION = "Zdjęcie bariery: Schody przy wejściu"
    }
}
