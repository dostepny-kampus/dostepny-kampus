/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.DpRect
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.MapAreaFixtures.area
import io.github.dostepnykampus.feature.map.MapAreaFixtures.twoAreas
import io.github.dostepnykampus.feature.map.area.center
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.overlay.MapOverlay
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.abs

/**
 * Podpis źródła planu piętra pod podpisem OpenStreetMap na nakładce mapy V1, z napisem zastępczym w miejscu
 * kolumny pięter, na prostokątach testowych (nie dane PWr).
 *
 * Wymagania: NFR-40, NFR-37, US16.1, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class MapOverlayPlanSourceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area(name = "Kampus główny", symbol = "A-1", west = 17.0)
    private val areas = twoAreas(main, area(name = "Wittigowo", symbol = "T-15", west = 17.03))

    /**
     * Wiersz: V1, przy widocznym planie piętra pod podpisem OpenStreetMap stoi „Źródło: Politechnika Wrocławska”,
     * w tym samym bloku, wyrównane do jego lewej krawędzi.
     *
     * Wymagania: NFR-40, NFR-37
     */
    @Test
    fun showsSourceUnderOsm() {
        compose.setContent { Overlay(hasPlan = true) }
        val osm = bounds(OSM)
        val source = bounds(SOURCE)
        assertTrue("źródło planu pod podpisem OSM", source.top >= osm.bottom)
        assertTrue("ta sama lewa krawędź", abs(osm.left.value - source.left.value) <= EDGE_TOLERANCE)
    }

    /** Wiersz: V1, bez planu piętra pod podpisem OpenStreetMap nie ma źródła PWr. Wymagania: NFR-40 */
    @Test
    fun hidesSourceWithoutPlan() {
        compose.setContent { Overlay(hasPlan = false) }
        compose.onNodeWithText(OSM).assertExists()
        compose.onNodeWithText(SOURCE).assertDoesNotExist()
    }

    /**
     * Wiersz: V1, pojawienie się i zniknięcie źródła planu nie przesuwa przycisków mapy, przycisku terenu ani „ⓘ”.
     *
     * Wymagania: NFR-40, NFR-20
     */
    @Test
    fun keepsButtonsWithSource() {
        var hasPlan by mutableStateOf(false)
        compose.setContent { Overlay(hasPlan = hasPlan) }
        val before = buttons()
        hasPlan = true
        compose.waitForIdle()
        assertEquals("przyciski po pokazaniu planu", before, buttons())
        hasPlan = false
        compose.waitForIdle()
        assertEquals("przyciski po ukryciu planu", before, buttons())
    }

    /**
     * Wiersz: V1, przy czcionce 200% na wąskim ekranie źródła planu na mapie nie ma (jest w legendzie pod „ⓘ”),
     * jeden krótki podpis OpenStreetMap mieści się cały przed „ⓘ”, a kolumna pięter kończy się nad nim.
     *
     * Wymagania: NFR-40, NFR-37, NFR-20, US16.1
     */
    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi", fontScale = 2f)
    fun hidesSourceAtLargeFont() {
        compose.setContent { Overlay(hasPlan = true) }
        compose.onNodeWithText(SOURCE).assertDoesNotExist()
        val osm = bounds(OSM_SHORT)
        val info = compose.onNodeWithContentDescription(INFO).getUnclippedBoundsInRoot()
        assertTrue("podpis OSM przed „ⓘ”", osm.right <= info.left)
        assertTrue("kolumna pięter nad podpisem OSM", bounds(FLOORS).bottom <= osm.top)
        assertFalse("podpis OSM bez ucięcia", layoutOf(OSM_SHORT).hasVisualOverflow)
    }

    @Test
    fun matchesLightMockup() {
        compose.setContent { Overlay(hasPlan = true) }
        compose.onRoot().captureRoboImage()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp-xhdpi", fontScale = 2f)
    fun matchesLargeFontMockup() {
        compose.setContent { Overlay(hasPlan = true) }
        compose.onRoot().captureRoboImage()
    }

    private fun bounds(text: String): DpRect =
        compose.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun layoutOf(text: String): TextLayoutResult {
        val layouts = mutableListOf<TextLayoutResult>()
        compose
            .onNodeWithText(text, useUnmergedTree = true)
            .performSemanticsAction(SemanticsActions.GetTextLayoutResult) { it(layouts) }
        return layouts.single()
    }

    private fun buttons(): List<DpRect> =
        listOf(
            compose.onNodeWithContentDescription(ZOOM_IN).getUnclippedBoundsInRoot(),
            compose.onNode(hasContentDescription(AREA, substring = true)).getUnclippedBoundsInRoot(),
            compose.onNodeWithContentDescription(INFO).getUnclippedBoundsInRoot(),
        )

    @Composable
    private fun Overlay(hasPlan: Boolean) {
        DostepnyKampusTheme {
            MapOverlay(
                camera = MapCamera(center = main.bounds.center, zoom = START_ZOOM),
                hasControls = true,
                dataDay = null,
                onCameraChange = {},
                areas = areas,
                onAreaShow = {},
                modifier = Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2)),
                hasPlanSource = hasPlan,
                start = { if (hasPlan) Text(text = FLOORS, modifier = Modifier.weight(weight = 1f)) },
                content = {},
            )
        }
    }

    private companion object {
        const val OSM = "© OpenStreetMap contributors"
        const val OSM_SHORT = "© OpenStreetMap"
        const val SOURCE = "Źródło: Politechnika Wrocławska"
        const val FLOORS = "Kolumna pięter"
        const val INFO = "Legenda i źródła mapy"
        const val ZOOM_IN = "Powiększ"
        const val AREA = "Zmień teren"
        const val EDGE_TOLERANCE = 0.5f
    }
}
