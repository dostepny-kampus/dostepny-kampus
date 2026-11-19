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
import androidx.compose.ui.test.performScrollTo
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
     * Wiersz: winda bez klatek w danych i zakres pięter z odmianą pod dojściem i drzwiami.
     * Scenariusz (częściowo, przykład: bez klatek w danych, 31 m, z parteru na 1 piętro): przegladanie-mapy.feature:
     *   Część „Za drzwiami” podaje najbliższą windę i jej piętra
     * Scenariusz (częściowo): przegladanie-mapy.feature: Opis wejścia zalecanego podaje dojście, drzwi
     *   i najbliższą windę
     */
    @Test
    fun showsLiftWithRange() {
        show(listOf("Parter", "1 piętro"))

        compose.onNodeWithText("Dojście").assertExists()
        compose.onNodeWithText("Drzwi").assertExists()
        compose.onNodeWithText("Za drzwiami").assertExists()
        compose.onNodeWithText("Winda, ok. 30 m od wejścia.").assertExists()
        compose.onNodeWithText("Jeździ od parteru do 1 piętra.").assertExists()
    }

    /**
     * Wiersz: nazwa piętra spoza odmiany daje listę pięter jak arkusz windy.
     * Scenariusz (częściowo, przykład: 12 m, staje na piwnicy, parterze i antresoli): przegladanie-mapy.feature:
     *   Część „Za drzwiami” podaje najbliższą windę i jej piętra
     */
    @Test
    fun listsFloorsWithoutRange() {
        show(listOf("Piwnica", "Parter", "Antresola"))

        compose.onNodeWithText("Jedzie na: piwnica, parter, antresola.").assertExists()
    }

    /** Zrzut arkusza przewiniętego do części „Za drzwiami” przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show(listOf("Piwnica", "Parter", "3 piętro"))
        compose.onNodeWithText("Jeździ od piwnicy do 3 piętra.").performScrollTo()
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
                approach = "Podjazd od parkingu.",
                doorWidthCm = 120,
            )
    }
}
