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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
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

    /**
     * Zwinięte piętro rozwija opis z funkcji pomieszczeń, a zwinięcie i rozwinięcie zmienia stan wiersza.
     *
     * Scenariusz: przegladanie-mapy.feature: Karta budynku szczegółowego opisuje piętra tekstem
     */
    @Test
    fun expandsFloorDescription() {
        compose.setContent { Group(FACTS) }
        compose.onNodeWithText("Portiernia, aula.").assertDoesNotExist()
        compose.onNodeWithText("Parter").assert(state("zwinięte")).performClick()
        compose.onNodeWithText("Portiernia, aula.").assertExists()
        compose.onNodeWithText("Parter").assert(state("rozwinięte"))
    }

    /**
     * Przycisk „Pokaż na planie” przy piętrze podaje identyfikator tego piętra.
     *
     * Scenariusz: przegladanie-mapy.feature: Karta budynku szczegółowego opisuje piętra tekstem
     */
    @Test
    fun showsPlanOfFloor() {
        val shown = mutableListOf<String>()
        compose.setContent { Group(FACTS, onShowPlan = { shown += it }) }
        compose.onNodeWithContentDescription("Pokaż na planie: Piwnica").performClick()
        assertEquals("„Pokaż na planie” nie dał piętra wiersza", listOf("P-1"), shown)
    }

    @Test
    fun saysBuildingHasNoPlan() {
        compose.setContent { Group(null, isBasic = true) }
        compose.onNodeWithText("Nie mamy planu pięter tego budynku.").assertExists()
    }

    /**
     * Bez danych z serwera grupa mówi, kiedy sprawdzimy plan, zamiast twierdzić, że go nie ma.
     *
     * Scenariusz: budynki-bez-szczegolow.feature: Bez danych z serwera karta nie twierdzi, że budynek nie ma planu
     *
     * Wymagania: US16.2, NFR-10
     */
    @Test
    fun saysPlanIsNotChecked() {
        compose.setContent { Group(null) }
        compose.onNodeWithText("Plan pięter sprawdzimy, gdy telefon połączy się z internetem.").assertExists()
        compose.onNodeWithText("Nie mamy planu pięter tego budynku.").assertDoesNotExist()
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
    @OptIn(ExperimentalMaterial3Api::class)
    private fun Group(facts: FloorFacts?, isBasic: Boolean = false, onShowPlan: (String) -> Unit = {}) {
        DostepnyKampusTheme {
            CompositionLocalProvider(LocalRippleConfiguration provides null) {
                Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                    FloorsGroup(facts = facts, isBasic = isBasic, onShowPlan = onShowPlan)
                }
            }
        }
    }

    private companion object {
        /** Czas, po którym kończą się fala dotknięcia i rozwinięcie opisu; dotyk nie zostawia fokusu. */
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
