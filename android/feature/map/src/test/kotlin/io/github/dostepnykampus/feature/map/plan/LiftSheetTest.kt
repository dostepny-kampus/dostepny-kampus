/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.feature.map.overlay.SheetExits
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Arkusz windy po dotknięciu znaku windy na planie. Wymagania: US16.1, US18.2, NFR-20, NFR-53 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LiftSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Wiersz: winda na planie ma nagłówek „Winda”, wiersz budynku, pod nim piętro osobnym wierszem i piętra windy
     *   z „(tutaj jesteś)”; wiersz budynku otwiera kartę budynku.
     * Scenariusz: przegladanie-mapy.feature: Arkusz windy ma wiersz budynku i piętro osobnym wierszem
     *
     * Wymagania: US16.1, US2.8
     */
    @Test
    fun showsLiftFloors() {
        val opened = mutableListOf<String>()
        compose.showPlanSheet(id = "winda", exits = SheetExits(onClose = {}, onBuilding = { opened += it }))

        compose.onNodeWithText("Winda").assertIsFocused()
        compose.onNodeWithText("Budynek A-1, parter").assertDoesNotExist()
        val building = compose.onNodeWithContentDescription("Budynek A-1").assertHeightIsAtLeast(48.dp)
        val floor = compose.onNodeWithText("Parter").getBoundsInRoot().top
        val stops = compose.onNodeWithText("Jedzie na: Parter (tutaj jesteś), 1 piętro").getBoundsInRoot().top
        assertTrue("piętro pod wierszem budynku: $floor", floor > building.getBoundsInRoot().top)
        assertTrue("„Jedzie na” pod piętrem: $stops > $floor", stops > floor)
        compose.onNodeWithText("Zamknij").assertExists()
        building.performClick()
        assertEquals("wiersz budynku", listOf("A-1"), opened)
    }

    /** Wiersz: przy dużej czcionce arkusz windy zajmuje cały ekran. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun fillsScreenAtLargeFont() {
        show()
        val top = compose.onNodeWithText("Zamknij").getBoundsInRoot().top

        assertTrue("góra arkusza przy górze ekranu: $top", top < 48.dp)
    }

    /** Zrzut arkusza windy przy czcionce 200%. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza windy, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza windy, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        show()
        compose.onRoot().captureRoboImage()
    }

    private fun show() = compose.showPlanSheet(id = "winda")
}
