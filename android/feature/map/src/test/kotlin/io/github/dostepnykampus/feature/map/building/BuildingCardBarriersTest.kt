/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import io.github.dostepnykampus.core.data.barrier.AcceptedBarrier
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant

/** Grupa „Bariery w budynku” na V2. Wymagania: US2.2, US2.4, BR-17 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h800dp")
class BuildingCardBarriersTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: zgłoszenie przyjęte budynku → pod „Bariery w budynku” typ, uciążliwość, miejsce z wejściem i piętrem
     *   oraz „Sprawdzone 12.11.2026”. Zdjęcia w grupie jeszcze nie ma.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Zgłoszenie przyjęte na karcie budynku ma zdjęcie i datę
     *   weryfikacji
     *
     * Wymagania: US2.2, BR-17
     */
    @Test
    fun us2_2_accepted_report_on_card() {
        compose.setContent { Card(listOf(BARRIER)) }

        compose.onNodeWithText("Bariery w budynku").performScrollTo().assertIsDisplayed()
        val texts = listOf("Blokada", "Schody przy wejściu", "Budynek A-1 · Główne, parter", "Sprawdzone 12.11.2026")
        texts.forEach { compose.onNodeWithText(it).performScrollTo().assertIsDisplayed() }
    }

    /** Wiersz: dwa zgłoszenia przyjęte → każde ma własny wiersz pod jednym nagłówkiem. Wymagania: US2.3 */
    @Test
    fun us2_3_two_reports() {
        val second = BARRIER.copy(id = "z2", type = BarrierType.STAIRS_AT_ENTRANCE, entranceId = null, floor = null)
        compose.setContent { Card(listOf(BARRIER, second)) }

        compose.onNodeWithText("Bariery w budynku").assertExists()
        compose.onAllNodesWithText("Sprawdzone 12.11.2026").assertCountEquals(2)
    }

    /**
     * Wiersz: budynek bez zgłoszeń przyjętych → karta nie ma grupy „Bariery w budynku”.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Karta budynku bez zgłoszeń przyjętych nie sugeruje braku
     *   barier
     *
     * Wymagania: US2.4
     */
    @Test
    fun us2_4_no_reports_no_group() {
        compose.setContent { Card(emptyList()) }

        compose.onNodeWithText("Wejścia").assertIsDisplayed()
        compose.onNodeWithText("Bariery w budynku").assertDoesNotExist()
    }

    @Composable
    private fun Card(barriers: List<AcceptedBarrier>) {
        val entrances = orderedEntrances(listOf(ENTRANCE))
        DostepnyKampusTheme {
            BuildingCardContent(
                symbol = "A-1",
                mode = CardMode.Expanded,
                card = BuildingCardUiState.Ready("A-1", "Gmach Główny", entrances, barriers),
                isOnline = true,
                onCheckEntrances = {},
                onShowOnMap = {},
                onBack = {},
                onClose = {},
                onReportEntrance = {},
                onRetry = {},
            )
        }
    }

    private companion object {
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
            )
    }
}
