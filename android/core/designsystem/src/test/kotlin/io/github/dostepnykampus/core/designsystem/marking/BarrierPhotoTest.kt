/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.designsystem.marking

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.intercept.Interceptor
import coil3.request.ErrorResult
import coil3.test.FakeImageLoaderEngine
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.submission.BarrierType
import kotlinx.coroutines.awaitCancellation
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

/** Wspólny komponent zdjęcia zgłoszenia bariery. Wymagania: US1.5, US8.4, BR-17, BR-19, NFR-12, NFR-18, NFR-50 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BarrierPhotoTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /** Wiersz: adres zdjęcia → obraz z opisem typu bariery. Wymagania: NFR-18 */
    @Test
    fun showsPhotoWithDescription() {
        show(PHOTO_URL, photoAvailable())

        compose.waitUntil(WAIT_MS) { shown(DESCRIPTION) }
        compose.onNodeWithContentDescription(DESCRIPTION).assertIsDisplayed()
    }

    /** Wiersz: brak adresu zdjęcia → brak obrazu i napisu o wczytywaniu. Wymagania: BR-17 */
    @Test
    fun hidesPhotoWithoutAddress() {
        show(null, photoAvailable())

        compose.waitForIdle()
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
        compose.onNodeWithText("Wczytujemy zdjęcie…").assertDoesNotExist()
    }

    /** Wiersz: zdjęcie się wczytuje → napis o wczytywaniu. Wymagania: NFR-12 */
    @Test
    fun showsLoadingNoteWhilePending() {
        show(PHOTO_URL, Interceptor { awaitCancellation() })

        compose.onNodeWithText("Wczytujemy zdjęcie…").assertIsDisplayed()
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
    }

    /** Wiersz: błąd pobrania → komunikat i „Spróbuj ponownie” o wysokości co najmniej 48 dp. Wymagania: NFR-50 */
    @Test
    fun showsErrorWithRetryOnFailure() {
        val failing = Interceptor { chain -> ErrorResult(null, chain.request, IOException("błąd")) }
        show(PHOTO_URL, failing)

        val message = "Nie udało się wczytać zdjęcia. Spróbuj ponownie za chwilę."
        compose.waitUntil(WAIT_MS) { shownText(message) }
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertHeightIsAtLeast(48.dp)
        compose.onNodeWithContentDescription(DESCRIPTION).assertDoesNotExist()
    }

    private fun shown(description: String) =
        compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()

    private fun shownText(text: String) = compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()

    @OptIn(DelicateCoilApi::class)
    private fun show(url: String?, loader: Interceptor) {
        val imageLoader = ImageLoader.Builder(compose.activity).components { add(loader) }.build()
        SingletonImageLoader.setUnsafe(imageLoader)
        compose.setContent {
            DostepnyKampusTheme { BarrierPhoto(url = url, type = BarrierType.STAIRS_AT_ENTRANCE) }
        }
    }

    private fun photoAvailable(): Interceptor =
        FakeImageLoaderEngine.Builder().intercept(PHOTO_URL, ColorImage(PHOTO_COLOR, width = 64, height = 48)).build()

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO_URL = "https://serwer.test/submissions/z1/photos/p1"
        const val PHOTO_COLOR = 0xFF336699.toInt()
        const val DESCRIPTION = "Zdjęcie bariery: Schody przy wejściu"
    }
}
