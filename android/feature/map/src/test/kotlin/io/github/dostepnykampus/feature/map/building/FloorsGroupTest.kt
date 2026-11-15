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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.building.Floor
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Grupa „Piętra” na karcie V2. Wymagania: US16.3, US16.2, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class FloorsGroupTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun describesFunctionsInOrder() {
        val functions = listOf(Counted("Sala dydaktyczna", 12), Counted("Laboratorium", 3), one("Portiernia"))
        assertEquals(
            "Opis piętra nie ma liczby w nawiasie albo kolejnej funkcji od małej litery",
            "Sala dydaktyczna (12), laboratorium (3), portiernia.",
            floorDescriptionText(functions),
        )
    }

    @Test
    fun expandsFloorDescription() {
        compose.setContent { Group(FACTS) }
        compose.onNodeWithText("Portiernia, aula.").assertDoesNotExist()
        compose.onNodeWithText("Parter").assert(state("zwinięte")).performClick()
        compose.onNodeWithText("Portiernia, aula.").assertExists()
        compose.onNodeWithText("Parter").assert(state("rozwinięte"))
    }

    @Test
    fun saysBuildingHasNoPlan() {
        compose.setContent { Group(null) }
        compose.onNodeWithText("Nie mamy planu pięter tego budynku.").assertExists()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku”, parter rozwinięty, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Group(FACTS) }
        compose.onNodeWithText("Parter").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(RIPPLE_END_MS)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku”, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        compose.setContent { Group(FACTS) }
        compose.onNodeWithText("Parter").performTouchInput { click() }
        compose.mainClock.advanceTimeBy(RIPPLE_END_MS)
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku-jasny-200”. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Group(FACTS) }
        compose.onRoot().captureRoboImage()
    }

    private fun state(text: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, text)

    private fun one(name: String) = Counted(name = name, count = 1)

    @Composable
    private fun Group(facts: FloorFacts?) {
        DostepnyKampusTheme {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                FloorsGroup(facts)
            }
        }
    }

    private companion object {
        /** Czas, po którym fala dotknięcia i rozwinięcie opisu na pewno się kończą; dotyk nie zostawia fokusu na zrzucie. */
        const val RIPPLE_END_MS = 2_000L

        val FACTS =
            FloorFacts(
                elevator = null,
                toilet = null,
                floors =
                    listOf(
                        FloorDescription(Floor(id = "P-1", label = "Piwnica", order = -1), emptyList()),
                        FloorDescription(
                            Floor(id = "P0", label = "Parter", order = 0),
                            listOf(Counted("Portiernia", 1), Counted("Aula", 1)),
                        ),
                        FloorDescription(
                            Floor(id = "P1", label = "1 piętro", order = 1),
                            listOf(Counted("Sala dydaktyczna", 8)),
                        ),
                    ),
            )
    }
}
