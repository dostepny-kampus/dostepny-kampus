/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapArea
import io.github.dostepnykampus.feature.map.data.MapCamera
import io.github.dostepnykampus.feature.map.data.START_ZOOM
import io.github.dostepnykampus.feature.map.data.center
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Przycisk sąsiedniego terenu na nakładce mapy V1 na prostokątach testowych (nie dane
 * PWr): „Wittigowo” leży około 2 km na wschód od terenu „Kampus główny”.
 *
 * Wymagania: US1.7, NFR-17, NFR-18
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h560dp-xhdpi")
class MapOverlayNeighborTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area("Kampus główny", west = 17.0)
    private val other = area("Wittigowo", west = 17.03)
    private val areas =
        MapAreas(
            data =
                CampusMapData(
                    buildings = JsonObject(emptyMap()),
                    labels = emptyList(),
                    labelPoints = JsonObject(emptyMap()),
                    entrances = JsonObject(emptyMap()),
                    start = null,
                    bounds = main.bounds,
                    areas = listOf(main, other),
                ),
            symbol = null,
            chosen = mutableStateOf(null),
            seen = mutableStateOf(null),
        )
    private val atEdge = GeoPoint(longitude = main.bounds.east, latitude = main.bounds.center.latitude)
    private val shown = mutableListOf<MapCamera>()

    /**
     * Wiersz: V1, na granicy terenu jest przycisk sąsiedniego terenu z nazwą dla TalkBack „Tu kończy się teren
     * Kampus główny. Obok: Wittigowo, 2 km” i celem dotyku 48 dp; stoi pod przyciskami mapy, więc ich nie
     * zasłania.
     *
     * Wymagania: US1.7, NFR-17, NFR-18
     */
    @Test
    fun us1_7_button_at_edge() {
        compose.setContent { Overlay(atEdge) }
        val button = compose.onNodeWithContentDescription(SPOKEN)
        button.assertHasClickAction().assertHeightIsAtLeast(48.dp)
        val controls = compose.onNodeWithContentDescription("Przesuń mapę w dół").getUnclippedBoundsInRoot()
        assertTrue(button.getUnclippedBoundsInRoot().top >= controls.bottom)
    }

    /**
     * Wiersz: V1, dotknięcie przycisku pokazuje sąsiedni teren: jego środek w powiększeniu startowym.
     *
     * Wymagania: US1.7
     */
    @Test
    fun us1_7_click_shows_neighbor() {
        compose.setContent { Overlay(atEdge) }
        compose.onNodeWithContentDescription(SPOKEN).performClick()
        assertEquals("Wittigowo", areas.current?.name)
        assertEquals(listOf(MapCamera(center = other.bounds.center, zoom = START_ZOOM)), shown)
    }

    /** Wiersz: V1, z dala od granicy przycisku sąsiedniego terenu nie ma. Wymagania: US1.7 */
    @Test
    fun us1_7_no_button_inside() {
        compose.setContent { Overlay(main.bounds.center) }
        compose.onNodeWithContentDescription(SPOKEN).assertDoesNotExist()
    }

    @Test
    fun us1_7_hifi_motyw_jasny() {
        compose.setContent { Overlay(atEdge) }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Overlay(center: GeoPoint) {
        DostepnyKampusTheme {
            MapOverlay(
                camera = MapCamera(center = center, zoom = START_ZOOM),
                hasControls = true,
                dataDay = null,
                onCameraChange = {},
                areas = areas,
                onAreaShow = { shown += it },
                modifier = Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2)),
                content = {},
            )
        }
    }

    private fun area(name: String, west: Double) =
        MapArea(
            name = name,
            symbols = listOf(name),
            bounds = CampusBounds(west = west, south = 51.0, east = west + SIDE, north = 51.0 + SIDE),
        )

    private companion object {
        const val SIDE = 0.01
        const val SPOKEN = "Tu kończy się teren Kampus główny. Obok: Wittigowo, 2 km"
    }
}
