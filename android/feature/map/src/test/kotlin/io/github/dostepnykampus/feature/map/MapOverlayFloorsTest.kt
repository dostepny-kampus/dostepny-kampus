/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.width
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.MapAreaFixtures.area
import io.github.dostepnykampus.feature.map.MapAreaFixtures.twoAreas
import io.github.dostepnykampus.feature.map.area.center
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.overlay.MapOverlay
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Miejsce kolumny pięter na nakładce mapy V1, z napisem zastępczym w miejscu kolumny, na prostokątach testowych
 * (nie dane PWr).
 *
 * Wymagania: US16.1, NFR-20
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class MapOverlayFloorsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area(name = "Kampus główny", symbol = "A-1", west = 17.0)
    private val other = area(name = "Wittigowo", symbol = "T-15", west = 17.03)
    private val areas = twoAreas(main, other)
    private val areaButton = hasContentDescription("Zmień teren", substring = true)

    /**
     * Wiersz: V1, kolumna pięter stoi przy lewej krawędzi mapy pod przyciskiem terenu i kończy się nad podpisem
     * OpenStreetMap.
     *
     * Wymagania: US16.1, NFR-20
     */
    @Test
    fun placesFloorsUnderAreaButton() {
        compose.setContent { Overlay() }
        val floors = compose.onNodeWithText(FLOORS).getUnclippedBoundsInRoot()
        val button = compose.onNode(areaButton).getUnclippedBoundsInRoot()
        val osm = compose.onNodeWithText("© OpenStreetMap contributors").getUnclippedBoundsInRoot()
        val width = compose.onRoot().getUnclippedBoundsInRoot().width
        assertTrue("kolumna pod przyciskiem terenu", floors.top >= button.bottom)
        assertTrue("kolumna przy lewej krawędzi", floors.right <= width / 2)
        assertTrue("kolumna nad podpisem OSM", floors.bottom <= osm.top)
    }

    /** Wiersz: V1, przy otwartym panelu terenu kolumny pięter nie ma. Wymagania: US16.1 */
    @Test
    fun hidesFloorsWithAreaPanel() {
        compose.setContent { Overlay() }
        compose.onNode(areaButton).performClick()
        compose.onNodeWithText("Teren na mapie").assertExists()
        compose.onNodeWithText(FLOORS).assertDoesNotExist()
    }

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
                modifier = Modifier.fillMaxSize(),
                start = { Text(FLOORS) },
                content = {},
            )
        }
    }

    private companion object {
        const val FLOORS = "Kolumna pięter"
    }
}
