/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.capture.report.marking

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.IMPORTANT_SIGN_TAG
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.feature.capture.photo.PhotoCamera
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

/** Ekran C6: obecne oznaczenie, wybór, zdjęcie, błędy walidacji i zapisu. Wymagania: US17.2, US17.3, BR-24, NFR-7 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MarkingContentTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<EntranceMarking>()
    private var sends = 0

    private fun show(state: MarkingUiState) {
        val camera = PhotoCamera(onTake = {})
        val actions = MarkingActions(camera = camera, onChoose = { chosen += it }, onSend = { sends++ }, onBack = {})
        compose.setContent { DostepnyKampusTheme { MarkingContent(state = state, actions = actions) } }
    }

    /** Wiersz: obecne oznaczenie słowem z datą i źródłem jak na V2, nazwa wejścia i budynek. Wymagania: US17.3 */
    @Test
    fun showsCurrentMarking() {
        show(MarkingUiState(entrance = SIDE, buildingName = "Gmach B", stage = MarkingStage.EDITING))

        compose.onNodeWithText("Obecne oznaczenie").assertIsDisplayed()
        compose.onNodeWithText("Brak danych").assertIsDisplayed()
        compose.onNodeWithText("Z map i opisów, stan z 3.11.2026").assertIsDisplayed()
        compose.onNodeWithText("Wejście boczne").assertIsDisplayed()
        compose.onNodeWithText("B-8 Gmach B").assertIsDisplayed()
    }

    /**
     * Wiersz: dwa wybory bez „Brak danych”; dotknięcie wybiera, a wybór obecnej wartości ją potwierdza.
     *
     * Wymagania: US17.2, BR-24, UC18
     */
    @Test
    fun choosesAndConfirms() {
        val current = SIDE.copy(marking = EntranceMarking.INACCESSIBLE)
        show(MarkingUiState(entrance = current, chosen = EntranceMarking.INACCESSIBLE, stage = MarkingStage.EDITING))

        compose.onNodeWithText("Dostępne").performClick()
        compose.onNodeWithText("Potwierdzasz obecne oznaczenie. Zmieni się data i źródło.").assertIsDisplayed()
        assertEquals("dotknięcie „Dostępne” wybiera je", listOf(EntranceMarking.ACCESSIBLE), chosen)
    }

    /**
     * Wiersz: wysłanie bez zdjęcia i wyboru pokazuje oba braki, bez pytania o typ bariery i uciążliwość.
     * Scenariusz (częściowo): oznaczenie-wejscia.feature:
     *   Zmiany oznaczenia wejścia nie da się zapisać bez zdjęcia
     *
     * Wymagania: US17.2, BR-7, BR-24
     */
    @Test
    fun showsValidationErrors() {
        show(MarkingUiState(entrance = SIDE, isChoiceMissing = true, isPhotoMissing = true))

        val errorAnnouncement = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Assertive)
        compose.onNodeWithText("Wybierz: dostępne czy niedostępne.").assertIsDisplayed().assert(errorAnnouncement)
        compose.onNodeWithText("Dodaj zdjęcie wejścia, aby wysłać zmianę.").assert(errorAnnouncement)
        compose.onAllNodesWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).assertCountEquals(3)
        compose.onNodeWithText("Typ bariery").assertDoesNotExist()
        compose.onNodeWithText("Uciążliwość").assertDoesNotExist()
    }

    /** Wiersz: wybrane oznaczenie ma stan „zaznaczone” dla TalkBack. Wymagania: NFR-18 */
    @Test
    fun marksChosenOption() {
        show(MarkingUiState(entrance = SIDE, chosen = EntranceMarking.ACCESSIBLE, stage = MarkingStage.EDITING))

        compose.onNodeWithText("Dostępne").assertIsSelected()
        compose.onAllNodesWithTag(IMPORTANT_SIGN_TAG, useUnmergedTree = true).assertCountEquals(1)
    }

    /** Wiersz: za mało miejsca w telefonie, „Spróbuj ponownie” wysyła jeszcze raz. Wymagania: NFR-7 */
    @Test
    fun offersRetryAfterSaveError() {
        show(MarkingUiState(entrance = SIDE, stage = MarkingStage.SAVE_FAILED))

        compose.onNodeWithText("Za mało miejsca w telefonie. Zwolnij miejsce i spróbuj ponownie.").assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("„Spróbuj ponownie” wysyła jeszcze raz", 1, sends)
    }

    private companion object {
        val SIDE =
            Entrance(
                id = "n/1",
                buildingSymbol = "B-8",
                name = "Wejście boczne",
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 17.06, latitude = 51.1),
                marking = EntranceMarking.NO_DATA,
                assessment = EntranceAssessment(date = LocalDate.of(2026, 11, 3), source = AssessmentSource.IMPORT),
            )
    }
}
