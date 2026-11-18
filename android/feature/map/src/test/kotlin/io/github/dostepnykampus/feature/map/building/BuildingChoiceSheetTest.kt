/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private val OUTLINE = GeoPolygon(listOf(listOf(GeoPoint(longitude = 0.0, latitude = 0.0))))

/** Arkusz „Który budynek?” z wierszami budynków. Wymagania: US2, US2.8, NFR-17, NFR-18 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class BuildingChoiceSheetTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val clicks = mutableListOf<String>()

    /**
     * Wiersz: każdy budynek to wiersz budynku w ramce, z nazwą pod napisem, co najmniej 48 dp; TalkBack czyta
     *   „Budynek C-3, nazwa” z czynnością „otwiera opis budynku”, a dotknięcie wybiera budynek.
     * Scenariusz (częściowo, przykład: kontur wspólny): przegladanie-mapy.feature: Wiersz budynku w arkuszu otwiera
     *   kartę budynku bez przelotu mapy
     *
     * Wymagania: US2, US2.8, NFR-17, NFR-18
     */
    @Test
    fun showsBuildingRows() {
        show()

        val row = compose.onNodeWithContentDescription("Budynek C-3, Wydział Chemiczny")
        row.assertHeightIsAtLeast(48.dp)
        val label = row.fetchSemanticsNode().config[SemanticsActions.OnClick].label
        assertEquals("czynność dla TalkBack", "otwiera opis budynku", label)
        row.performClick()
        compose.onNodeWithContentDescription("Budynek C-4").performClick()
        assertEquals("wybrane budynki", listOf("C-3", "C-4"), clicks)
    }

    /** Wiersz: „Zamknij” zamyka arkusz bez wyboru budynku. Wymagania: NFR-53 */
    @Test
    fun closesWithoutChoice() {
        show()

        compose.onNodeWithText("Zamknij").performClick()
        assertEquals("zamknięcie", listOf("zamknij"), clicks)
    }

    /** Zrzut arkusza „Który budynek?”, motyw jasny. */
    @Test
    fun matchesLightMockup() {
        show()
        compose.onRoot().captureRoboImage()
    }

    /** Zrzut arkusza „Który budynek?” przy czcionce 200%: znak budynku znika, napis i strzałka zostają. */
    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        show()
        compose.onRoot().captureRoboImage()
    }

    private fun show() {
        val buildings =
            listOf(
                Building(symbol = "C-3", name = "Wydział Chemiczny", outline = OUTLINE),
                Building(symbol = "C-4", name = null, outline = OUTLINE),
            )
        compose.setContent {
            DostepnyKampusTheme {
                Box(Modifier.fillMaxSize().background(Color(0xFFBEE3F2))) {
                    val bottom = Modifier.align(Alignment.BottomCenter)
                    BuildingChoiceSheet(onClose = { clicks += "zamknij" }, modifier = bottom) {
                        buildings.forEach { building -> ChoiceLink(building) { clicks += building.symbol } }
                    }
                }
            }
        }
    }
}
