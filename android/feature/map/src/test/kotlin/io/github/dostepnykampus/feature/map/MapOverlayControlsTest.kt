/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.designsystem.FloorOption
import io.github.dostepnykampus.core.designsystem.FloorOptions
import io.github.dostepnykampus.core.designsystem.FloorPicker
import io.github.dostepnykampus.core.designsystem.FloorPickerLayout
import io.github.dostepnykampus.core.designsystem.Odstepy
import io.github.dostepnykampus.feature.map.MapAreaFixtures.area
import io.github.dostepnykampus.feature.map.MapAreaFixtures.twoAreas
import io.github.dostepnykampus.feature.map.area.center
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Przyciski mapy obok przycisku terenu na nakładce mapy V1, z kolumną pięter planu testowego, na prostokątach
 * testowych (nie dane PWr).
 *
 * Wymagania: NFR-20, NFR-53, NFR-17, US1.7
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w320dp-h640dp-xhdpi")
class MapOverlayControlsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area(name = "Kampus główny", symbol = "A-1", west = 17.0)
    private val areas = twoAreas(main, area(name = SHORT, symbol = "T-15", west = 17.03))
    private val areaButton = hasContentDescription("Zmień teren", substring = true)

    /**
     * Wiersz: V1, przy zwykłej czcionce na ekranie 360 dp przyciski mapy stoją u góry obok przycisku terenu.
     *
     * Wymagania: NFR-53, US1.7
     */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xhdpi")
    fun keepsControlsBesideArea() {
        compose.setContent { Overlay() }
        val area = compose.onNode(areaButton).getUnclippedBoundsInRoot()
        val up = compose.onNodeWithContentDescription(UP).getUnclippedBoundsInRoot()
        assertTrue("„w górę” w wierszu przycisku terenu", up.top < area.bottom)
        assertNoOverlap()
    }

    /**
     * Wiersz: V1, przy czcionce 200% na ekranie 320 dp przycisk terenu nie mieści się obok przycisków mapy, więc
     * przyciski stoją pod nim, każdy z celem co najmniej 48 dp, i nie nachodzą na kolumnę pięter ani podpis mapy.
     *
     * Wymagania: NFR-20, NFR-53, NFR-17, US16.1
     */
    @Test
    @Config(fontScale = 2f)
    fun dropsControlsUnderArea() {
        compose.setContent { Overlay() }
        val area = compose.onNode(areaButton).getUnclippedBoundsInRoot()
        val buttons = controls()
        assertTrue("nazwa głównego kampusu mieści się w jednym wierszu", area.bottom - area.top <= TOUCH)
        assertTrue("przyciski pod przyciskiem terenu", buttons.all { it.top >= area.bottom })
        assertNoOverlap()
        assertTrue(
            "cel dotyku co najmniej 48 dp",
            buttons.all { it.right - it.left >= TOUCH && it.bottom - it.top >= TOUCH },
        )
        val floorLabels =
            FLOORS.map { label ->
                compose
                    .onNodeWithContentDescription(label)
                    .assertIsDisplayed()
                    .getUnclippedBoundsInRoot()
            }
        val osm = compose.onNodeWithText(OSM).getUnclippedBoundsInRoot()
        val info = compose.onNodeWithContentDescription(INFO).getUnclippedBoundsInRoot()
        buttons.forEach { button ->
            assertFalse("przycisk na kolumnie pięter", floorLabels.any { button.overlaps(it) })
            assertTrue("przycisk nad podpisem mapy i „ⓘ”", button.bottom <= osm.top && button.bottom <= info.top)
        }
    }

    /**
     * Wiersz: V1, po zmianie terenu na krótszą nazwę i z powrotem przyciski mapy już w pierwszej klatce stoją
     * tam, gdzie nie zachodzą na przycisk terenu.
     *
     * Wymagania: NFR-20, US1.7
     */
    @Test
    @Config(fontScale = 2f)
    fun keepsFramesClearOnAreaChange() {
        compose.setContent { Overlay() }
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        changeArea(SHORT)
        val shortArea = compose.onNode(areaButton).getUnclippedBoundsInRoot()
        val up = compose.onNodeWithContentDescription(UP).getUnclippedBoundsInRoot()
        assertTrue("krótka nazwa mieści się obok przycisków", up.top < shortArea.bottom)
        changeArea(main.name)
    }

    @Test
    @Config(fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Overlay() }
        compose.onRoot().captureRoboImage()
    }

    private fun changeArea(name: String) {
        compose.runOnUiThread { areas.show(name) }
        repeat(FRAMES) {
            compose.mainClock.advanceTimeByFrame()
            assertNoOverlap()
        }
        compose.onNodeWithContentDescription("Teren: $name. Zmień teren").assertExists()
    }

    private fun controls(): List<DpRect> =
        CONTROLS.map { compose.onNodeWithContentDescription(it).getUnclippedBoundsInRoot() }

    private fun assertNoOverlap() {
        val area = compose.onNode(areaButton).getUnclippedBoundsInRoot()
        controls().forEach { assertFalse("przycisk mapy na przycisku terenu", it.overlaps(area)) }
    }

    private fun DpRect.overlaps(other: DpRect): Boolean =
        left < other.right && other.left < right && top < other.bottom && other.top < bottom

    @Composable
    private fun Overlay() {
        DostepnyKampusTheme {
            MapOverlay(
                camera = MapCamera(center = main.bounds.center, zoom = START_ZOOM),
                hasControls = true,
                dataDay = null,
                onCameraChange = {},
                areas = areas,
                onAreaShow = {},
                modifier = Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2)),
                start = {
                    FloorPicker(
                        options = FloorOptions(FLOORS.map { FloorOption(key = it, label = it) }),
                        selectedKey = "0",
                        onSelect = {},
                        layout = FloorPickerLayout.COLUMN,
                        modifier = Modifier.weight(weight = 1f, fill = false).padding(Odstepy.s),
                    )
                },
                content = {},
            )
        }
    }

    private companion object {
        const val SHORT = "Ta"
        const val UP = "Przesuń mapę w górę"
        const val INFO = "Legenda i źródła mapy"
        const val OSM = "© OpenStreetMap"
        const val FRAMES = 3
        val TOUCH = 48.dp
        val FLOORS = listOf("3", "2", "1", "0", "-1")
        val CONTROLS =
            listOf(
                UP,
                "Przesuń mapę w dół",
                "Przesuń mapę w lewo",
                "Przesuń mapę w prawo",
                "Powiększ",
                "Pomniejsz",
            )
    }
}
