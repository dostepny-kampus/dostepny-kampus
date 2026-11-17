/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
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

    /** Wiersz: winda na planie ma nagłówek „Winda”, budynek z piętrem i piętra windy z „(tutaj jesteś)”. */
    @Test
    fun showsLiftFloors() {
        show()

        compose.onNodeWithText("Winda").assertIsFocused()
        compose.onNodeWithText("Budynek A-1, parter").assertExists()
        compose.onNodeWithText("Jedzie na: Parter (tutaj jesteś), 1 piętro").assertExists()
        compose.onNodeWithText("Zamknij").assertExists()
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

    private fun show() = compose.showPlanSheet(id = "winda", onClose = {})
}
