/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.inspection.progress

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.inspection.BuildingProgress
import io.github.dostepnykampus.core.model.inspection.InspectionCoverage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/**
 * K1 „Postęp inwentaryzacji”: lista budynków szczegółowych i podstawowych, „Otwórz”, „Odśwież” i stany wspólne.
 *
 * Wymagania: US11.1, US11.2, US11.3, NFR-50
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class ProgressScreenTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Przykład „4 z 5 pięter, 80%” z wymagań K1 z liczbami zgłoszeń i wejść.
     *
     * Wymagania: US11.1
     */
    @Test
    fun showsDetailedBuilding() {
        show(ready(DETAILED))
        compose.onNodeWithText("A-1").assertIsDisplayed()
        compose.onNodeWithText("4 z 5 pięter, 80%").assertIsDisplayed()
        compose.onNodeWithText("Czeka na sprawdzenie: 3").assertIsDisplayed()
        compose.onNodeWithText("Przyjęte: 4").assertIsDisplayed()
        compose.onNodeWithText("Wejścia sprawdzone na miejscu: 1 z 6").assertIsDisplayed()
    }

    /**
     * Budynek podstawowy: „Obejrzany” albo „Nieobejrzany”, bez wejść sprawdzonych na miejscu.
     *
     * Wymagania: US11.1
     */
    @Test
    fun showsBasicBuildings() {
        show(ready(BASIC_SEEN, BASIC_UNSEEN))
        compose.onNodeWithText("Obejrzany").assertIsDisplayed()
        compose.onNodeWithText("Nieobejrzany").assertIsDisplayed()
        compose.onAllNodesWithText("Wejścia sprawdzone na miejscu", substring = true).assertCountEquals(0)
    }

    /**
     * „Otwórz” przy budynku przekazuje jego symbol do C5.
     *
     * Wymagania: US11.3
     */
    @Test
    fun openPassesSymbol() {
        val opened = mutableListOf<String>()
        show(ready(DETAILED), actions = ProgressActions(onBack = {}, onOpen = { opened.add(it) }))
        compose.onNodeWithText("Otwórz").performClick()
        assertEquals("symbol otwartego budynku", listOf("A-1"), opened)
    }

    /**
     * „Odśwież” czyta postęp od nowa, a obok stoi chwila odczytu.
     *
     * Wymagania: US11.2
     */
    @Test
    fun refreshReadsAgain() {
        var refreshes = 0
        show(ready(DETAILED), onRefresh = { refreshes++ })
        compose.onNodeWithText("Stan z", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Odśwież").performClick()
        assertEquals("liczba odświeżeń", 1, refreshes)
    }

    /**
     * W trakcie odświeżania „Odśwież” jest wyłączony.
     *
     * Wymagania: US11.2
     */
    @Test
    fun refreshDisabledWhileBusy() {
        show(ready(DETAILED, isRefreshing = true))
        compose.onNodeWithText("Odśwież").assertIsNotEnabled()
    }

    /**
     * Wczytywanie: „Wczytujemy postęp…”.
     *
     * Wymagania: US11.2
     */
    @Test
    fun showsLoading() {
        show(ProgressUiState.Loading)
        compose.onNodeWithText("Wczytujemy postęp…").assertIsDisplayed()
    }

    /**
     * Bez internetu: linia, komunikat i „Spróbuj ponownie” odświeża.
     *
     * Wymagania: NFR-50
     */
    @Test
    fun offlineOffersRetry() {
        var refreshes = 0
        show(ProgressUiState.Offline, onRefresh = { refreshes++ })
        compose.onNodeWithText("Bez internetu").assertIsDisplayed()
        compose.onNodeWithText("Ta czynność wymaga internetu", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").performClick()
        assertEquals("liczba ponowień", 1, refreshes)
    }

    /**
     * Błąd serwera: komunikat i „Spróbuj ponownie”.
     *
     * Wymagania: US11.2
     */
    @Test
    fun serverErrorOffersRetry() {
        show(ProgressUiState.ServerError)
        compose.onNodeWithText("Serwer chwilowo nie odpowiada", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Spróbuj ponownie").assertIsDisplayed()
    }

    /**
     * Brak uprawnień: komunikat i „Wróć do mapy”.
     *
     * Wymagania: US11.2
     */
    @Test
    fun forbiddenLeadsBack() {
        var back = 0
        show(ProgressUiState.Forbidden, actions = ProgressActions(onBack = { back++ }, onOpen = {}))
        compose.onNodeWithText("Nie masz już uprawnień", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Wróć do mapy").performClick()
        assertEquals("liczba powrotów", 1, back)
    }

    private fun show(
        state: ProgressUiState,
        actions: ProgressActions = ProgressActions(onBack = {}, onOpen = {}),
        onRefresh: () -> Unit = {},
    ) {
        compose.setContent {
            DostepnyKampusTheme { ProgressContent(state = state, actions = actions, onRefresh = onRefresh) }
        }
    }

    private fun ready(vararg buildings: BuildingProgress, isRefreshing: Boolean = false): ProgressUiState.Ready =
        ProgressUiState.Ready(buildings = buildings.toList(), readAt = READ_AT, isRefreshing = isRefreshing)

    private companion object {
        val READ_AT: Instant = Instant.parse("2026-11-12T10:42:00Z")
        val DETAILED =
            BuildingProgress(
                buildingSymbol = "A-1",
                coverage = InspectionCoverage.Floors(floorsTotal = 5, floorsInspected = 4),
                pendingSubmissions = 3,
                acceptedSubmissions = 4,
                entrancesTotal = 6,
                entrancesCheckedOnSite = 1,
                markers = emptyList(),
            )
        val BASIC_SEEN = basic(symbol = "A-6", isInspected = true)
        val BASIC_UNSEEN = basic(symbol = "H-5", isInspected = false)

        fun basic(symbol: String, isInspected: Boolean): BuildingProgress =
            BuildingProgress(
                buildingSymbol = symbol,
                coverage = InspectionCoverage.Whole(isInspected = isInspected),
                pendingSubmissions = 0,
                acceptedSubmissions = 1,
                entrancesTotal = 2,
                entrancesCheckedOnSite = 0,
                markers = emptyList(),
            )
    }
}
