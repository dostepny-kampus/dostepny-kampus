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
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private const val PLACE = "Od strony C-20, od północy."
private const val RAMP = "Do wejścia prowadzi rampa."
private const val DOOR = "Szerokość drzwi: 100 cm"

/**
 * Wejścia bez nazwy w danych na V2: nazwa „Wejście”, pod nią opis położenia, potem uwaga uczelni i szerokość
 * drzwi, a zdanie pod nagłówkiem podaje położenie wejścia zalecanego.
 *
 * Wymagania: US2.1, US3.1, BR-25, BR-27
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class PlaceDescriptionListTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: V2, wejście bez nazwy z opisem położenia, uwagą i drzwiami 100 cm → w wierszu od góry „Wejście”,
     *   opis położenia, uwaga i szerokość drzwi.
     * Scenariusz (częściowo): przegladanie-mapy.feature: Wejście bez nazwy w danych ma na karcie opis położenia pod nazwą
     *
     * Wymagania: US2.1, BR-27
     */
    @Test
    fun listsPlaceBeforeRemark() {
        group(listOf(unnamed(id = "pwr/1", approach = RAMP, doorWidthCm = 100)))
        val tops =
            listOf("Wejście", PLACE, RAMP, DOOR).map {
                compose.onNodeWithText(it, useUnmergedTree = true).getUnclippedBoundsInRoot().top
            }
        assertTrue("Od góry: nazwa, położenie, uwaga, drzwi, a są na $tops", tops == tops.sorted())
    }

    /**
     * Wiersz: V2, zalecane „Wejście” z opisem położenia i drzwiami 100 cm → zdanie „Zalecane wejście od strony C-20,
     *   od północy. Drzwi 100 cm.”, bez cudzysłowu.
     *
     * Wymagania: US3.1, BR-25, BR-27
     */
    @Test
    fun answersWithPlace() {
        header(listOf(unnamed(id = "pwr/1", approach = "", doorWidthCm = 100)))
        compose.onNode(hasText("Zalecane wejście od strony C-20, od północy. Drzwi 100 cm.")).assertExists()
    }

    /**
     * Wiersz: V2, zalecane „Wejście główne” z opisem położenia → zdanie podaje nazwę, a nie położenie.
     *
     * Wymagania: US3.1, BR-25, BR-27
     */
    @Test
    fun answersMainByName() {
        val main = unnamed(id = "pwr/1", approach = "", doorWidthCm = 0).copy(name = "Wejście główne", isMain = true)
        header(listOf(main))
        compose.onNode(hasText("Zalecane: „Wejście główne”.")).assertExists()
    }

    /** Zrzut listy wejść z opisem położenia, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        group(MIXED)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut listy wejść z opisem położenia przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        group(MIXED)
        compose.onRoot().captureRoboImage()
    }

    private fun group(entrances: List<Entrance>) {
        val card = ready(entrances)
        compose.setContent {
            Themed {
                EntrancesGroup(card = card, withAssessment = true, onShowEntrance = {}, onReport = {})
            }
        }
    }

    private fun header(entrances: List<Entrance>) {
        val card = ready(entrances)
        compose.setContent {
            Themed {
                CardHeader(symbol = "C-19", card = card, mode = CardMode.Expanded, onClose = {})
            }
        }
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
                unnamed(id = "pwr/1", approach = RAMP, doorWidthCm = 100),
                unnamed(id = "pwr/2", approach = "", doorWidthCm = 0).copy(marking = EntranceMarking.NO_DATA),
            )

        fun ready(entrances: List<Entrance>): BuildingCardUiState.Ready =
            BuildingCardUiState.Ready(symbol = "C-19", name = null, entrances = orderedEntrances(entrances))

        fun unnamed(id: String, approach: String, doorWidthCm: Int): Entrance =
            Entrance(
                id = id,
                buildingSymbol = "C-19",
                name = "Wejście",
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                marking = EntranceMarking.ACCESSIBLE,
                approach = approach,
                doorWidthCm = doorWidthCm,
                isNameGenerated = true,
                placeDescription = PLACE,
            )
    }
}
