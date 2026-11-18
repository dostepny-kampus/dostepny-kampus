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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.AssessmentSource
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceAssessment
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

private const val CESARSKIE = "Wejście Cesarskie"
private const val APPROACH = "Do wejścia prowadzi pochylnia."
private const val PWR_SOURCE = "Z planów Politechniki Wrocławskiej, stan z 12.10.2024"

/**
 * Wejścia z planów PWr na V2: nazwa, opis dojścia i szerokość drzwi na liście „Wejścia”, podpis źródła z datą
 * i zdanie-odpowiedź z szerokością drzwi wejścia zalecanego.
 *
 * Wymagania: US2.1, US3.1, US17.3, BR-24, BR-27
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PwrEntranceListTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, wejście z planów PWr z opisem dojścia i szerokością 120 cm → w wierszu wejścia nazwa, opis
     *   dojścia i „Szerokość drzwi: 120 cm”.
     *
     * Wymagania: US2.1, BR-27
     */
    @Test
    fun showsApproachAndDoorWidth() {
        group(listOf(pwr(id = "pwr/1", name = CESARSKIE, approach = APPROACH, doorWidthCm = 120)))
        compose.onNodeWithText(CESARSKIE).assertExists()
        compose.onNodeWithText(APPROACH, useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Szerokość drzwi: 120 cm", useUnmergedTree = true).assertExists()
    }

    /**
     * Wiersz: V2, wejście bez szerokości drzwi w danych (serwer pomija szerokość 0) i bez opisu → w wierszu nie ma
     *   szerokości drzwi ani pustego opisu, jest sama nazwa.
     *
     * Wymagania: US2.1, BR-27
     */
    @Test
    fun skipsMissingDoorWidth() {
        group(listOf(pwr(id = "pwr/1", name = "Wejście", approach = null, doorWidthCm = null)))
        compose.onNodeWithText("Wejście", useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("Szerokość drzwi", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    /**
     * Wiersz: V2, wejście z planów PWr z dniem edycji warstwy 12.10.2024 → napis źródła
     *   „Z planów Politechniki Wrocławskiej, stan z 12.10.2024”, nie „Z map i opisów”.
     *
     * Wymagania: US17.3, BR-24, BR-27
     */
    @Test
    fun namesPwrSource() {
        group(listOf(pwr(id = "pwr/1", name = CESARSKIE, approach = APPROACH, doorWidthCm = 120)))
        compose.onNodeWithText(PWR_SOURCE, useUnmergedTree = true).assertExists()
        compose.onAllNodes(hasText("Z map i opisów", substring = true), useUnmergedTree = true).assertCountEquals(0)
    }

    /**
     * Wiersz: V2, wejście zalecane z planów PWr z drzwiami 120 cm → zdanie „Zalecane: „Wejście Cesarskie”.
     *   Drzwi 120 cm.”; plany nie podają odległości do windy, więc zdanie kończy się na drzwiach.
     *
     * Wymagania: US3.1, BR-25, BR-27
     */
    @Test
    fun answersWithDoorWidth() {
        header(listOf(pwr(id = "pwr/1", name = CESARSKIE, approach = APPROACH, doorWidthCm = 120)))
        compose.onNode(hasText("Zalecane: „$CESARSKIE”. Drzwi 120 cm.")).assertExists()
    }

    /**
     * Wiersz: V2, wejście zalecane bez szerokości drzwi → zdanie bez części o drzwiach.
     *
     * Wymagania: US3.1, BR-27
     */
    @Test
    fun answersWithoutDoorWidth() {
        header(listOf(pwr(id = "pwr/1", name = CESARSKIE, approach = null, doorWidthCm = null)))
        compose.onNode(hasText("Zalecane: „$CESARSKIE”.")).assertExists()
        compose.onAllNodes(hasText("Drzwi", substring = true)).assertCountEquals(0)
    }

    /** Zrzut listy wejść z planów PWr, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        group(MIXED)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut listy wejść z planów PWr przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        group(MIXED)
        compose.onRoot().captureRoboImage()
    }

    private fun group(entrances: List<Entrance>) {
        val card = ready(entrances)
        compose.setContent { Themed { EntrancesGroup(card, withAssessment = true, onShowEntrance = {}, onReport = {}) } }
    }

    private fun header(entrances: List<Entrance>) {
        val card = ready(entrances)
        compose.setContent { Themed { CardHeader(symbol = "A-1", card = card, mode = CardMode.Expanded, onClose = {}) } }
    }

    @Composable
    private fun Themed(content: @Composable () -> Unit) {
        DostepnyKampusTheme {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) { content() }
        }
    }

    private companion object {
        val MIXED =
            listOf(
                pwr(id = "pwr/1", name = CESARSKIE, approach = APPROACH, doorWidthCm = 120),
                pwr(id = "pwr/2", name = "Wejście", approach = "Do wejścia prowadzą schody.", doorWidthCm = null)
                    .copy(marking = EntranceMarking.INACCESSIBLE),
            )

        fun ready(entrances: List<Entrance>): BuildingCardUiState.Ready =
            BuildingCardUiState.Ready(symbol = "A-1", name = null, entrances = orderedEntrances(entrances))

        fun pwr(id: String, name: String, approach: String?, doorWidthCm: Int?): Entrance =
            Entrance(
                id = id,
                buildingSymbol = "A-1",
                name = name,
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = EntranceMarking.ACCESSIBLE,
                assessment = EntranceAssessment(LocalDate.of(2024, 10, 12), AssessmentSource.PWR_PLANS),
                approach = approach,
                doorWidthCm = doorWidthCm,
            )
    }
}
