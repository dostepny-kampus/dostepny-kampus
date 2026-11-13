/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.searchBuildings
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.MapArea
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Wynik wyszukiwania V1 z innego terenu (US1.8, NFR-18) na budynkach testowych „Budynek testowy” (nie dane PWr):
 * mapa pokazuje teren „Kampus główny”, a „T-15” leży na terenie „Wittigowo”.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h400dp-xhdpi")
class SearchOtherAreaTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val catalog =
        BuildingCatalog(buildings = listOf("A-1", "T-15").map(::building), entrances = emptyList(), attribution = "")
    private val shown = area(name = "Kampus główny", symbol = "A-1")
    private val areas = AreaOrder(listOf(shown, area(name = "Wittigowo", symbol = "T-15")))

    /**
     * Wiersz: V1, wynik budynku z innego terenu ma dopisek „Wittigowo · inny teren”, a TalkBack czyta go razem
     * z wynikiem (US1.8, NFR-18).
     */
    @Test
    fun us1_8_result_other_area_note() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("t15")
        compose.onNodeWithContentDescription("T-15, Budynek testowy, teren Wittigowo, inny teren").assertExists()
        compose.onNodeWithText("Wittigowo · inny teren", useUnmergedTree = true).assertExists()
    }

    /** Wiersz: V1, wynik budynku z terenu pokazanego na mapie nie ma dopisku o terenie (US1.8). */
    @Test
    fun us1_8_result_shown_area_plain() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("a1")
        compose.onNodeWithContentDescription("A-1, Budynek testowy").assertExists()
    }

    @Test
    fun us1_8_hifi_motyw_jasny() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("t15")
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Search() {
        var search by remember { mutableStateOf(BuildingSearch()) }
        DostepnyKampusTheme {
            CampusSearch(
                catalog = catalog,
                search = search,
                onSearch = { query -> search = BuildingSearch(query, searchBuildings(query, catalog.buildings)) },
                isOnline = true,
                syncedAt = null,
                isList = false,
                areas = areas,
                onListChange = {},
                onBuildingClick = {},
            )
        }
    }

    private fun area(name: String, symbol: String) =
        MapArea(name, listOf(symbol), CampusBounds(west = 0.0, south = 0.0, east = 1.0, north = 1.0))

    private fun building(symbol: String): Building {
        val corners = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
        val outline = GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        return Building(symbol = symbol, name = "Budynek testowy", outline = outline)
    }
}
