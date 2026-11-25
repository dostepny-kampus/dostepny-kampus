/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import coil3.ColorImage
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.test.FakeImageLoaderEngine
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.building.EntranceDistance
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Ekran C7: dane zgłoszenia przyjętego, nowe zdjęcie, błąd walidacji i błąd zapisu. Wymagania: US8.4, BR-19, NFR-7 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class RemovedContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private fun show(state: RemovedUiState, onSend: () -> Unit = {}) {
        val actions = RemovedActions(camera = PhotoCamera(onTake = {}), onSend = onSend, onBack = {})
        compose.setContent { DostepnyKampusTheme { RemovedContent(state = state, actions = actions) } }
    }

    /** Wiersz: nazwa budynku i wejścia z katalogu stoi w miejscu zgłoszenia przyjętego. Wymagania: US8.4, US1.5 */
    @Test
    fun showsBuildingAndEntranceNames() {
        val state = RemovedUiState(previous = LIFT, buildingName = "Gmach Główny", entranceName = "Wejście Cesarskie")
        show(state.copy(stage = RemovedStage.EDITING))

        compose.onNodeWithText("A-1 Gmach Główny · Wejście Cesarskie, piętro 1").assertIsDisplayed()
    }

    /** Wiersz: zgłoszenie przyjęte bez edycji, nowe zdjęcie i napis o mapie. Wymagania: US8.4, BR-19 */
    @Test
    fun showsAcceptedAndNewPhoto() {
        var sends = 0
        show(RemovedUiState(previous = LIFT, stage = RemovedStage.EDITING)) { sends++ }

        compose.onNodeWithText("Zgłoś, że bariery nie ma").assertIsDisplayed()
        compose.onNodeWithText("Niesprawna winda").assertIsDisplayed()
        compose.onNodeWithText("Budynek A-1 · piętro 1").assertIsDisplayed()
        compose.onNodeWithText("Sprawdzone 7.10.2026").assertIsDisplayed()
        compose.onNodeWithText("Bariera zostanie na mapie", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Wyślij do sprawdzenia").performClick()
        assertEquals("„Wyślij do sprawdzenia” wysyła raz", 1, sends)
    }

    /** Wiersz: miejsce słowami od najbliższego wejścia, jak na C2, bez współrzędnych. Wymagania: US8.4, NFR-19 */
    @Test
    fun showsPlaceInWords() {
        val entrance =
            Entrance(
                id = "n/1",
                buildingSymbol = "A-1",
                name = "Wejście dostosowane",
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 17.0621, latitude = 51.1079),
                marking = EntranceMarking.ACCESSIBLE,
            )
        val near = EntranceDistance(entrance = entrance, meters = 24.6)
        show(RemovedUiState(previous = LIFT, nearest = near, stage = RemovedStage.EDITING))

        compose.onNodeWithText("ok. 25 m od wejścia „Wejście dostosowane”").assertIsDisplayed()
    }

    /** Wiersz: wysłanie bez zdjęcia pokazuje błąd walidacji. Wymagania: BR-19 */
    @Test
    fun showsValidationError() {
        show(RemovedUiState(previous = LIFT, isPhotoMissing = true, stage = RemovedStage.EDITING))

        compose.onNodeWithText("Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.").assertIsDisplayed()
    }

    /** Wiersz: za mało miejsca w telefonie, „Spróbuj ponownie” wysyła jeszcze raz. Wymagania: NFR-7 */
    @Test
    fun offersRetryAfterSaveError() {
        var sends = 0
        show(RemovedUiState(previous = LIFT, stage = RemovedStage.SAVE_FAILED)) { sends++ }

        compose.onNodeWithText("Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("„Spróbuj ponownie” wysyła jeszcze raz", 1, sends)
    }

    /** Wiersz: zgłoszenie przyjęte ze zdjęciem → C7 pokazuje zdjęcie pod danymi zgłoszenia. Wymagania: US8.4, BR-19 */
    @OptIn(DelicateCoilApi::class)
    @Test
    fun showsAcceptedPhoto() {
        val image = ColorImage(PHOTO_COLOR, width = 64, height = 48)
        val engine = FakeImageLoaderEngine.Builder().intercept(PHOTO_URL, image).build()
        SingletonImageLoader.setUnsafe(ImageLoader.Builder(compose.activity).components { add(engine) }.build())
        show(RemovedUiState(previous = LIFT.copy(photoUrl = PHOTO_URL), stage = RemovedStage.EDITING))

        val description = "Zdjęcie bariery: Niesprawna winda"
        compose.waitUntil(WAIT_MS) {
            compose.onAllNodesWithContentDescription(description).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithContentDescription(description).assertIsDisplayed()
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO_URL = "https://serwer.test/submissions/tb9/photos/p1"
        const val PHOTO_COLOR = 0xFF336699.toInt()
        val LIFT =
            AcceptedBarrier(
                id = "tb9",
                location = GeoPoint(longitude = 17.06209, latitude = 51.10785),
                severity = Severity.BLOCKING,
                type = BarrierType.BROKEN_LIFT,
                buildingSymbol = "A-1",
                verifiedAt = Instant.parse("2026-10-07T10:00:00Z"),
                floor = SubmissionFloor(number = 1),
            )
    }
}
