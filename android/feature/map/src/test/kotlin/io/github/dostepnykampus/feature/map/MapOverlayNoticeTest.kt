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
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.feature.map.area.MapAreas
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.CampusMapData
import io.github.dostepnykampus.feature.map.data.MapArea
import io.github.dostepnykampus.feature.map.data.MapCamera
import io.github.dostepnykampus.feature.map.data.START_ZOOM
import io.github.dostepnykampus.feature.map.data.center
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Napis o zmianie terenu na nakładce mapy V1 na prostokątach testowych (nie dane PWr):
 * zaznaczony budynek „T-15” leży na terenie „Wittigowo”, a mapa pokazywała „Kampus główny”.
 *
 * Wymagania: US1.8, NFR-18, NFR-23
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h560dp-xhdpi")
class MapOverlayNoticeTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = area(name = "Kampus główny", symbol = "A-1", west = 17.0)
    private val other = area(name = "Wittigowo", symbol = "T-15", west = 17.03)
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
            symbol = "T-15",
            chosen = mutableStateOf(null),
            seen = mutableStateOf(null),
        )

    /**
     * Wiersz: V1, po zmianie terenu przez zaznaczony budynek nad podpisem mapy stoi napis „Pokazuję teren:
     * Wittigowo”, który TalkBack czyta sam.
     *
     * Wymagania: US1.8, NFR-18
     */
    @Test
    fun us1_8_notice_above_attribution() {
        compose.setContent { Overlay() }
        val notice = compose.onNodeWithText("Pokazuję teren: Wittigowo")
        notice.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.LiveRegion))
        val attribution = compose.onNodeWithText("© OpenStreetMap contributors").getUnclippedBoundsInRoot()
        assertTrue(notice.getUnclippedBoundsInRoot().bottom <= attribution.top)
    }

    /**
     * Wiersz: V1, napis o zmianie terenu znika, gdy osoba ruszy mapę przyciskiem, bez limitu czasu.
     *
     * Wymagania: US1.8, NFR-23
     */
    @Test
    fun us1_8_notice_gone_after_move() {
        compose.setContent { Overlay() }
        compose.onNodeWithContentDescription("Przesuń mapę w górę").performClick()
        compose.onNodeWithText("Pokazuję teren: Wittigowo").assertDoesNotExist()
    }

    @Test
    fun us1_8_hifi_motyw_jasny() {
        compose.setContent { Overlay() }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Overlay() {
        DostepnyKampusTheme {
            MapOverlay(
                camera = MapCamera(center = other.bounds.center, zoom = START_ZOOM),
                hasControls = true,
                dataDay = null,
                onCameraChange = {},
                areas = areas,
                onAreaShow = {},
                modifier = Modifier.fillMaxSize().background(Color(color = 0xFFBEE3F2)),
                content = {},
            )
        }
    }

    private fun area(name: String, symbol: String, west: Double) =
        MapArea(
            name = name,
            symbols = listOf(symbol),
            bounds = CampusBounds(west = west, south = 51.0, east = west + SIDE, north = 51.0 + SIDE),
        )

    private companion object {
        const val SIDE = 0.01
    }
}
