/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.feature.map.PHOTO_URL
import io.github.dostepnykampus.feature.map.photoAvailable
import io.github.dostepnykampus.feature.map.usePhotoLoader
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Grupa „Bariery w budynku” na V2. Wymagania: US2.2, US2.4, US2.7, BR-17 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardBarriersTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: zgłoszenie przyjęte budynku → pod „Bariery w budynku” typ, uciążliwość, miejsce z wejściem i piętrem
     *   oraz „Sprawdzone 12.11.2026” i zdjęcie z opisem.
     * Scenariusz: przegladanie-mapy.feature: Zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę
     *   weryfikacji
     *
     * Wymagania: US2.2, BR-17, NFR-18
     */
    @Test
    fun showsAcceptedReportOnCard() {
        show(listOf(BARRIER))

        compose.onNodeWithText("Bariery w budynku").performScrollTo().assertIsDisplayed()
        val texts = listOf("Blokada", "Schody przy wejściu", "Budynek A-1 · Główne, parter", "Sprawdzone 12.11.2026")
        texts.forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
        compose.waitUntil(WAIT_MS) { photoShown() }
        compose.onNodeWithContentDescription(PHOTO).performScrollTo().assertIsDisplayed()
    }

    /**
     * Wiersz: dane o barierach są, budynek bez zgłoszeń przyjętych → pod „Bariery w budynku” ramka „Brak zgłoszonych
     *   barier” ze zdaniem, kiedy bariera się pojawi.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Karta budynku bez zgłoszeń przyjętych nie sugeruje braku
     *   barier
     *
     * Wymagania: US2.4
     */
    @Test
    fun showsEmptyStateWithoutReports() {
        show(emptyList())

        compose.onNodeWithText("Bariery w budynku").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Brak zgłoszonych barier").performScrollTo().assertIsDisplayed()
        val sentence = "Bariera pojawi się tutaj, gdy ktoś ją zgłosi i zostanie sprawdzona."
        compose.onNodeWithText(sentence).performScrollTo().assertIsDisplayed()
    }

    /**
     * Wiersz: telefon bez danych o barierach → karta nie ma grupy „Bariery w budynku” ani stanu pustego.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Bez danych o barierach lista i karta budynku nic o barierach
     *   nie mówią
     *
     * Wymagania: US2.7
     */
    @Test
    fun hidesGroupWithoutData() {
        show(null)

        compose.onNodeWithText("Wejścia").assertIsDisplayed()
        compose.onNodeWithText("Bariery w budynku").assertDoesNotExist()
        compose.onNodeWithText("Brak zgłoszonych barier").assertDoesNotExist()
    }

    /**
     * Wiersz: Zbierający widzi przy zgłoszeniu przyjętym „Zgłoś, że bariery nie ma”; dotknięcie podaje jego
     *   identyfikator, a bez uprawnienia przycisku nie ma.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Inny Zbierający niż autor zgłasza usunięcie bariery
     *
     * Wymagania: US8.4, BR-19
     */
    @Test
    fun collectorSeesRemovalButton() {
        val reported = mutableListOf<String>()
        show(listOf(BARRIER)) { reported.add(it) }

        compose.onNodeWithText("Zgłoś, że bariery nie ma").performScrollTo().performClick()

        assertEquals("Przycisk podaje identyfikator zgłoszenia", listOf("z1"), reported)
    }

    /** Wiersz: bez uprawnienia Zbierającego przycisku „Zgłoś, że bariery nie ma” nie ma. Wymagania: US8.4 */
    @Test
    fun noRoleHidesRemovalButton() {
        show(listOf(BARRIER))

        compose.onNodeWithText("Zgłoś, że bariery nie ma").assertDoesNotExist()
    }

    /**
     * Wiersz: Zbierający widzi „Popraw zgłoszenie” przy własnym zgłoszeniu przyjętym i przy przyjętym szybkim,
     *   a nie widzi go przy cudzym zgłoszeniu precyzyjnym.
     * Scenariusz (częściowo): zgloszenia-zbierajacego.feature: Zmiana zgłoszenia przyjętego tworzy nowe zgłoszenie
     *
     * Wymagania: BR-3, BR-18
     */
    @Test
    fun replacesOwnAndQuickOnly() {
        val replaced = mutableListOf<String>()
        val quick = BARRIER.copy(id = "z-quick", isQuick = true)
        val other = BARRIER.copy(id = "z-other", isQuick = false)
        show(
            barriers = listOf(BARRIER, quick, other),
            ownBarrierIds = setOf("z1"),
            onReplaceBarrier = { replaced.add(it) },
        )

        val buttons = compose.onAllNodesWithText("Popraw zgłoszenie")
        assertEquals("Dwa przyciski: dla własnego i dla szybkiego", 2, buttons.fetchSemanticsNodes().size)
        buttons[0].performScrollTo().performClick()
        buttons[1].performScrollTo().performClick()
        assertEquals("identyfikatory poprawianych zgłoszeń", listOf("z1", "z-quick"), replaced)
    }

    /** Wiersz: bez roli Zbierającego przycisku „Popraw zgłoszenie” nie ma. Wymagania: BR-3, BR-18 */
    @Test
    fun noRoleHidesReplaceButton() {
        show(barriers = listOf(BARRIER.copy(isQuick = true)), ownBarrierIds = setOf("z1"))

        compose.onNodeWithText("Popraw zgłoszenie").assertDoesNotExist()
    }

    private fun photoShown() = compose.onAllNodesWithContentDescription(PHOTO).fetchSemanticsNodes().isNotEmpty()

    private fun show(
        barriers: List<AcceptedBarrier>?,
        ownBarrierIds: Set<String> = emptySet(),
        onReplaceBarrier: ((String) -> Unit)? = null,
        onReportRemoval: ((String) -> Unit)? = null,
    ) {
        usePhotoLoader(compose.activity, photoAvailable())
        val entrances = orderedEntrances(listOf(ENTRANCE))
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    card =
                        BuildingCardUiState.Ready(
                            symbol = "A-1",
                            name = "Gmach Główny",
                            entrances = entrances,
                            barriers = barriers,
                            ownBarrierIds = ownBarrierIds,
                        ),
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                    onReportRemoval = onReportRemoval,
                    onReplaceBarrier = onReplaceBarrier,
                )
            }
        }
    }

    private companion object {
        const val WAIT_MS = 5_000L
        const val PHOTO = "Zdjęcie bariery: Schody przy wejściu"
        val ENTRANCE =
            Entrance(
                id = "a1-main",
                buildingSymbol = "A-1",
                name = "Główne",
                isMain = true,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = EntranceMarking.ACCESSIBLE,
            )

        val BARRIER =
            AcceptedBarrier(
                id = "z1",
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                severity = Severity.BLOCKING,
                type = BarrierType.STAIRS_AT_ENTRANCE,
                buildingSymbol = "A-1",
                verifiedAt = Instant.parse("2026-11-12T10:00:00Z"),
                entranceId = "a1-main",
                floor = SubmissionFloor(number = 0),
                photoIds = listOf("p1"),
                photoUrl = PHOTO_URL,
            )
    }
}
