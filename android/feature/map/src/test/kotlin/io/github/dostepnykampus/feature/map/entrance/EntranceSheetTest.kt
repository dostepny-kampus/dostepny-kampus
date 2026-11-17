/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Arkusz wejścia na V1 po dotknięciu znaku wejścia. Wymagania: US2.1, US3.3, US17.1, NFR-18, NFR-53 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class EntranceSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    /** Wiersz: arkusz wejścia, nagłówek z fokusem, słowo oznaczenia z dopiskiem, nazwa i przyciski. */
    @Test
    fun showsMainEntranceWithName() {
        compose.setContent { Sheet(entrance(EntranceMarking.ACCESSIBLE, name = "Wejście Cesarskie", isMain = true)) }

        compose.onNodeWithText("Wejście, budynek A-1").assertIsFocused()
        compose.onNodeWithText("Dostępne, wejście główne").assertExists()
        compose.onNodeWithText("Wejście Cesarskie").assertExists()
        compose.onNodeWithText("Nikt jeszcze nie sprawdził tego wejścia.").assertDoesNotExist()
        compose.onNodeWithText("Zamknij").assertHeightIsAtLeast(48.dp).performClick()
        compose.onNodeWithText("Sprawdź, którędy wejść").performClick()
        assertEquals("zamknij, potem V3 budynku", listOf("zamknij", "A-1"), clicks)
    }

    /** Wiersz: wejście bez nazwy w danych nie ma nazwy, a „Niedostępne” stoi bez dopisku. Wymagania: US2.1 */
    @Test
    fun showsInaccessibleWithoutName() {
        compose.setContent { Sheet(entrance(EntranceMarking.INACCESSIBLE, name = null, isMain = false)) }

        compose.onNodeWithText("Niedostępne").assertExists()
        compose.onNodeWithText("wejście główne", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Wejście bez nazwy w danych").assertDoesNotExist()
    }

    /** Wiersz: przy „Brak danych” zdanie, że nikt nie sprawdził wejścia. Wymagania: US17.1 */
    @Test
    fun showsUncheckedEntrance() {
        compose.setContent { Sheet(entrance(EntranceMarking.NO_DATA, name = null, isMain = false)) }

        compose.onNodeWithText("Brak danych").assertExists()
        compose.onNodeWithText("Nikt jeszcze nie sprawdził tego wejścia.").assertExists()
    }

    /** Wiersz: przy czcionce 200% arkusz się przewija, a przycisk V3 jest osiągalny. Wymagania: NFR-20 */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Sheet(shown()) }

        compose.onNodeWithText("Sprawdź, którędy wejść").performScrollTo().assertExists()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza wejścia, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        compose.setContent { Sheet(shown()) }
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza wejścia, motyw ciemny. */
    @Test
    @Config(qualifiers = "+night")
    fun matchesDarkMockup() {
        compose.setContent { Sheet(shown()) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Sheet(entrance: Entrance) {
        DostepnyKampusTheme {
            Box(Modifier.fillMaxSize().background(MapGround)) {
                EntranceSheet(
                    entrance = entrance,
                    onClose = { clicks += "zamknij" },
                    onEntrancesClick = { clicks += it },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }

    private fun shown() = entrance(EntranceMarking.ACCESSIBLE, name = "Wejście Cesarskie", isMain = true)

    private fun entrance(marking: EntranceMarking, name: String?, isMain: Boolean) =
        Entrance(
            id = "node/1",
            buildingSymbol = "A-1",
            name = name,
            isMain = isMain,
            type = null,
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            marking = marking,
        )

    private companion object {
        val MapGround = Color(0xFFBEE3F2)
    }
}
