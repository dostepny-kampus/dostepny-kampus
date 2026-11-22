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
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Grupa „Winda i toaleta” na karcie V2 budynku szczegółowego. Wymagania: US16.3, NFR-20 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LiftToiletGroupTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    /**
     * Grupa „Winda i toaleta” podaje tekstem piętra z windą i toaletą, a piętra bez nich wymienia osobno.
     *
     * Scenariusz: przegladanie-mapy.feature: Karta budynku szczegółowego opisuje piętra tekstem
     */
    @Test
    fun showsLiftAndToiletFloors() {
        compose.setContent { Group() }
        compose.onNodeWithText("Na każdym piętrze: Piwnica – 3 piętro.").assertExists()
        compose
            .onNodeWithText(
                "Dla osób z niepełnosprawnością. Na piętrach: Piwnica, 1 piętro (2) i 2 piętro. " +
                    "Nie ma na piętrach: Parter i 3 piętro.",
            ).assertExists()
    }

    @Test
    fun hidesGroupWithoutLiftAndToilet() {
        compose.setContent { Group(FloorFacts(elevator = null, toilet = null, floors = emptyList())) }
        compose.onNodeWithText("Winda i toaleta").assertDoesNotExist()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku”, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Group() }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku”, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        compose.setContent { Group() }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut do porównania z makietą hi-fi „m-v2-karta-budynku-jasny-200”. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Group() }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Group(facts: FloorFacts = FACTS) {
        DostepnyKampusTheme {
            Column(Modifier.background(MaterialTheme.colorScheme.surface).padding(16.dp)) {
                LiftToiletGroup(facts)
            }
        }
    }

    private companion object {
        val FACTS =
            FloorFacts(
                elevator =
                    FloorPresence(
                        floors = listOf("Piwnica", "Parter", "1 piętro", "2 piętro", "3 piętro").map { Counted(it, 1) },
                        missing = emptyList(),
                    ),
                toilet =
                    FloorPresence(
                        floors = listOf(Counted("Piwnica", 1), Counted("1 piętro", 2), Counted("2 piętro", 1)),
                        missing = listOf("Parter", "3 piętro"),
                    ),
                floors = emptyList(),
            )
    }
}
