/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.removed

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
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

    /** Wiersz: zgłoszenie przyjęte bez edycji, nowe zdjęcie i napis o mapie. Wymagania: US8.4, BR-19 */
    @Test
    fun us8_4_accepted_and_new_photo() {
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

    /** Wiersz: wysłanie bez zdjęcia pokazuje błąd walidacji. Wymagania: BR-19 */
    @Test
    fun br19_validation_error() {
        show(RemovedUiState(previous = LIFT, isPhotoMissing = true, stage = RemovedStage.EDITING))

        compose.onNodeWithText("Dodaj zdjęcie miejsca, aby wysłać zgłoszenie.").assertIsDisplayed()
    }

    /** Wiersz: za mało miejsca w telefonie, „Spróbuj ponownie” wysyła jeszcze raz. Wymagania: NFR-7 */
    @Test
    fun nfr7_save_error_retry() {
        var sends = 0
        show(RemovedUiState(previous = LIFT, stage = RemovedStage.SAVE_FAILED)) { sends++ }

        compose.onNodeWithText("Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("„Spróbuj ponownie” wysyła jeszcze raz", 1, sends)
    }

    private companion object {
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
