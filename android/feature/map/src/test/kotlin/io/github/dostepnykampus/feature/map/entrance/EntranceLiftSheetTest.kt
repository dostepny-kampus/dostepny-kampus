/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Część „Za drzwiami” arkusza wejścia. Wymagania: US3.3, US16.3, BR-25 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceLiftSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: winda bez klatek w danych i zakres pięter z odmianą.
     * Scenariusz (częściowo, przykład: bez klatek): przegladanie-mapy.feature: Część „Za drzwiami” podaje
     *   najbliższą windę i jej piętra
     */
    @Test
    fun showsLiftWithRange() {
        show(listOf("Piwnica", "Parter", "3 piętro"))

        compose.onNodeWithText("Za drzwiami").assertExists()
        compose.onNodeWithText("Winda, ok. 30 m od wejścia.").assertExists()
        compose.onNodeWithText("Jeździ od piwnicy do 3 piętra.").assertExists()
    }

    /** Wiersz: nazwa piętra spoza odmiany daje listę pięter jak arkusz windy. */
    @Test
    fun listsFloorsWithoutRange() {
        show(listOf("Piwnica", "Parter", "Antresola"))

        compose.onNodeWithText("Jedzie na: piwnica, parter, antresola.").assertExists()
    }

    /** Zrzut arkusza z częścią „Za drzwiami” przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show(listOf("Piwnica", "Parter", "3 piętro"))
        compose.onRoot().captureRoboImage()
    }

    private fun show(floors: List<String>) {
        val lift = EntranceLift(entranceId = ENTRANCE.id, meters = 30, floors = floors)
        compose.setContent { EntranceSheetOnMap(ShownEntrance(entrance = ENTRANCE, isRecommended = true, lift = lift)) }
    }

    private companion object {
        val ENTRANCE =
            Entrance(
                id = "node/1",
                buildingSymbol = "A-1",
                name = "Wejście Cesarskie",
                isMain = false,
                type = null,
                location = GeoPoint(longitude = 17.06, latitude = 51.11),
                marking = EntranceMarking.ACCESSIBLE,
                doorWidthCm = 120,
            )
    }
}
