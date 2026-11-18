/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.EvacuationExit
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val BUTTON = "Pokaż wyjścia ewakuacyjne (2)"
private const val ROOM_41 = "Wyjście ewakuacyjne z pomieszczenia 41"
private const val STAIRS = "Wyjście ewakuacyjne na klatkę schodową"

/**
 * Wyjścia ewakuacyjne z planów PWr na V2: przycisk z liczbą pod grupą „Wejścia”, lista rozwijana jak piętra,
 * w wierszu opis i szerokość drzwi z danych, bez dotyku wiersza.
 *
 * Wymagania: US2.6, BR-27, NFR-17, NFR-18
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EvacuationExitsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2 budynku z 2 wyjściami w danych → pod grupą „Wejścia”, nad „Piętra”, przycisk
     *   „Pokaż wyjścia ewakuacyjne (2)”; lista na start zwinięta.
     *
     * Wymagania: US2.6, BR-27
     */
    @Test
    fun showsCountUnderEntrances() {
        card(EXITS)
        val entrance = compose.onNodeWithText("Wejście główne", useUnmergedTree = true).getUnclippedBoundsInRoot()
        val button = compose.onNodeWithText(BUTTON, useUnmergedTree = true).getUnclippedBoundsInRoot()
        val floors = compose.onNodeWithText("Piętra", useUnmergedTree = true).getUnclippedBoundsInRoot()
        assertTrue("przycisk nie stoi pod wejściami", button.top >= entrance.bottom && button.bottom <= floors.top)
        compose.onAllNodes(hasText(ROOM_41), useUnmergedTree = true).assertCountEquals(0)
    }

    /**
     * Wiersz: dotknięcie „Pokaż wyjścia ewakuacyjne (2)” → lista obu wyjść z opisem; „Szerokość drzwi: 90 cm” tylko
     *   przy wyjściu z szerokością w danych; wiersz wyjścia niczego nie otwiera.
     *
     * Wymagania: US2.6, BR-27
     */
    @Test
    fun expandsExitList() {
        exits(EXITS)
        compose.onNodeWithText(BUTTON, useUnmergedTree = true).performClick()
        compose.onNodeWithText(ROOM_41, useUnmergedTree = true).assertExists()
        compose.onNodeWithText(STAIRS, useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("Szerokość drzwi: 90 cm"), useUnmergedTree = true).assertCountEquals(1)
        compose.onAllNodes(hasClickAction()).assertCountEquals(1)
    }

    /**
     * Wiersz: V2 budynku bez wyjść ewakuacyjnych w danych → przycisku nie ma.
     *
     * Wymagania: US2.6
     */
    @Test
    fun hidesWithoutExits() {
        card(emptyList())
        compose.onAllNodes(hasText("Pokaż wyjścia", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    /** Zrzut rozwiniętej listy wyjść ewakuacyjnych, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        expandedShot()
    }

    /** Zrzut rozwiniętej listy wyjść ewakuacyjnych przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        expandedShot()
    }

    private fun expandedShot() {
        exits(EXITS)
        compose.onNodeWithText(BUTTON).performSemanticsAction(SemanticsActions.OnClick)
        compose.onRoot().captureRoboImage()
    }

    private fun exits(exits: List<EvacuationExit>) {
        compose.setContent {
            DostepnyKampusTheme {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    EvacuationExits(exits = exits)
                }
            }
        }
    }

    private fun card(exits: List<EvacuationExit>) {
        val card =
            BuildingCardUiState.Ready(
                symbol = "A-1",
                name = null,
                entrances = orderedEntrances(listOf(ENTRANCE)),
                evacuationExits = exits,
            )
        compose.setContent {
            DostepnyKampusTheme {
                BuildingCardContent(
                    symbol = "A-1",
                    mode = CardMode.Expanded,
                    card = card,
                    isOnline = true,
                    onShowOnMap = {},
                    onBack = {},
                    onClose = {},
                    onReport = {},
                    onRetry = {},
                )
            }
        }
    }

    private companion object {
        val ORIGIN = GeoPoint(longitude = 0.5, latitude = 0.5)
        val EXITS =
            listOf(
                EvacuationExit(id = "pwr/9", location = ORIGIN, description = ROOM_41, doorWidthCm = 90),
                EvacuationExit(id = "pwr/10", location = ORIGIN, description = STAIRS),
            )
        val ENTRANCE =
            Entrance(
                id = "pwr/1",
                buildingSymbol = "A-1",
                name = "Wejście główne",
                isMain = true,
                type = null,
                location = ORIGIN,
                marking = EntranceMarking.ACCESSIBLE,
            )
    }
}
