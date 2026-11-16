/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.model.inspection.ProgressMarker
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * C5 „Obejrzane piętra” Koordynatora: autor przy znaczniku, „Cofnij znacznik” z potwierdzeniem, „Zostaw znacznik”.
 *
 * Wymagania: US11.3, BR-20
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class MarkersScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Przy znaczniku widać piętro, autora i postęp budynku.
     *
     * Wymagania: US11.3, BR-5
     */
    @Test
    fun showsMarkerWithAuthor() {
        show(MarkersUiState.Ready(BUILDING))
        compose.onNodeWithText(FLOOR).assertIsDisplayed()
        compose.onNodeWithText("Znacznik: Zbierający, Autor 14").assertIsDisplayed()
        compose.onNodeWithText("Obejrzane piętra: 4 z 5").assertIsDisplayed()
    }

    /**
     * Potwierdzenie cofnięcia mówi, że postęp budynku się zmniejszy.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Koordynator cofa znacznik dowolnego Zbierającego po potwierdzeniu
     *
     * Wymagania: US11.3
     */
    @Test
    fun confirmationWarnsAboutProgress() {
        show(MarkersUiState.Ready(BUILDING, asking = MARKER_ID))
        compose.onNodeWithText("Postęp budynku się zmniejszy", substring = true).assertIsDisplayed()
        compose.onNodeWithText(KEEP).assertIsDisplayed()
    }

    /** Wiersz: znacznik piętra pokazuje etykietę z danych PWr, a pytanie o cofnięcie też. Wymagania: US11.3 */
    @Test
    fun showsFloorLabelNotId() {
        show(MarkersUiState.Ready(BUILDING, asking = MARKER_ID, floorLabels = mapOf("2" to "Parter")))
        compose.onNodeWithText("Piętro Parter").assertIsDisplayed()
        compose.onNodeWithText("Cofnąć znacznik piętra Parter?", substring = true).assertIsDisplayed()
    }

    /**
     * „Cofnij znacznik” przy znaczniku przekazuje jego identyfikator.
     *
     * Wymagania: US11.3
     */
    @Test
    fun askPassesMarkerId() {
        val asked = mutableListOf<String>()
        show(MarkersUiState.Ready(BUILDING), actions(onAsk = { asked.add(it) }))
        compose.onNodeWithText(REVOKE).performClick()
        assertEquals("pytanie o znacznik", listOf(MARKER_ID), asked)
    }

    /**
     * „Zostaw znacznik” i „Cofnij znacznik” w potwierdzeniu wywołują swoje czynności.
     *
     * Scenariusz: postep-inwentaryzacji.feature: Bez potwierdzenia znacznik zostaje
     *
     * Wymagania: US11.3
     */
    @Test
    fun confirmationButtonsCallActions() {
        val calls = mutableListOf<String>()
        val handlers = actions(onKeep = { calls.add(KEEP) }, onConfirm = { calls.add(REVOKE) })
        show(MarkersUiState.Ready(BUILDING, asking = MARKER_ID), handlers)
        compose.onNodeWithText(KEEP).performClick()
        compose.onNodeWithText(REVOKE).performClick()
        assertEquals("kolejność czynności", listOf(KEEP, REVOKE), calls)
    }

    /**
     * Błąd cofnięcia bez internetu: komunikat o internecie, znacznik zostaje na liście.
     *
     * Wymagania: US11.3, NFR-50
     */
    @Test
    fun offlineRevokeShowsMessage() {
        show(MarkersUiState.Ready(BUILDING, failure = RevokeFailure.OFFLINE))
        compose.onNodeWithText("Ta czynność wymaga internetu", substring = true).assertIsDisplayed()
        compose.onNodeWithText(FLOOR).assertIsDisplayed()
    }

    /**
     * Błąd serwera przy odczycie: komunikat i „Spróbuj ponownie”.
     *
     * Wymagania: US11.3, NFR-50
     */
    @Test
    fun serverErrorOffersRetry() {
        var retries = 0
        show(MarkersUiState.ServerError, actions(onRetry = { retries++ }))
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("liczba ponowień", 1, retries)
    }

    private fun actions(
        onAsk: (String) -> Unit = {},
        onKeep: () -> Unit = {},
        onConfirm: () -> Unit = {},
        onRetry: () -> Unit = {},
    ) = MarkerActions(onAsk = onAsk, onKeep = onKeep, onConfirm = onConfirm, onRetry = onRetry)

    private fun show(state: MarkersUiState, actions: MarkerActions = actions()) {
        compose.setContent { DostepnyKampusTheme { MarkersContent(state = state, actions = actions, onBack = {}) } }
    }

    private companion object {
        const val FLOOR = "Piętro 2"
        const val SYMBOL = "A-1"
        const val KEEP = "Zostaw znacznik"
        const val REVOKE = "Cofnij znacznik"
        const val MARKER_ID = "7f0c1c1e-0000-4000-8000-000000000001"
        val MARKER =
            InspectionMarker(
                id = MARKER_ID,
                buildingSymbol = SYMBOL,
                place = MarkerPlace.Floor(floorId = "2"),
                createdAt = Instant.parse("2026-11-12T10:00:00Z"),
                isRevoked = false,
                isMine = false,
            )
        val BUILDING =
            BuildingProgress(
                buildingSymbol = SYMBOL,
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4),
                pendingSubmissions = 3,
                acceptedSubmissions = 4,
                entrancesTotal = 6,
                entrancesCheckedOnSite = 1,
                markers = listOf(ProgressMarker(marker = MARKER, authorPseudonym = "Autor 14")),
            )
    }
}
