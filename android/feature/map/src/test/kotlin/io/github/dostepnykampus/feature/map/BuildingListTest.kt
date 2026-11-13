/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.searchBuildings
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.data.CampusBounds
import io.github.dostepnykampus.feature.map.data.MapArea
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Lista budynków V1 z nagłówkami terenów na budynkach testowych „Budynek testowy” (nie dane
 * PWr): mapa pokazuje teren „Wittigowo”, więc stoi on na liście pierwszy.
 *
 * Wymagania: US1.8, NFR-18, NFR-19
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class BuildingListTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val catalog =
        BuildingCatalog(
            buildings = listOf("A-1", "T-15", "C-13", "T-16").map(::building),
            entrances = emptyList(),
            attribution = "",
        )
    private val areas =
        AreaOrder(listOf(area("Wittigowo", "T-15", "T-16"), area("Kampus główny", "A-1", "C-13")))
    private val chosen = mutableListOf<String>()

    /**
     * Wiersz: V1, lista budynków ma nagłówki terenów z liczbą budynków, teren pokazany na mapie pierwszy; nagłówek
     * jest nagłówkiem dla TalkBack.
     *
     * Wymagania: US1.8, NFR-18, NFR-19
     */
    @Test
    fun us1_8_area_headings_in_order() {
        compose.setContent { List(query = "") }
        val first = compose.onNodeWithContentDescription("Wittigowo, 2 budynki")
        val second = compose.onNodeWithContentDescription("Kampus główny, 2 budynki")
        first.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        second.assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.Heading))
        val building = compose.onNodeWithContentDescription("T-16, Budynek testowy").getUnclippedBoundsInRoot()
        assertTrue(first.getUnclippedBoundsInRoot().bottom <= building.top)
        assertTrue(building.bottom <= second.getUnclippedBoundsInRoot().top)
    }

    /**
     * Wiersz: V1, wpisany symbol zawęża listę, a teren bez pasującego budynku nie ma nagłówka.
     *
     * Wymagania: US1.8, NFR-19
     */
    @Test
    fun us1_8_query_drops_empty_area() {
        compose.setContent { List(query = "c13") }
        compose.onNodeWithContentDescription("Kampus główny, 2 budynki").assertExists()
        compose.onNodeWithContentDescription("Wittigowo, 2 budynki").assertDoesNotExist()
        compose.onNodeWithContentDescription("T-15, Budynek testowy").assertDoesNotExist()
    }

    /** Wiersz: V1, budynek z innego terenu na liście zgłasza swój symbol. Wymagania: US1.8 */
    @Test
    fun us1_8_building_click_reported() {
        compose.setContent { List(query = "") }
        compose.onNodeWithContentDescription("A-1, Budynek testowy").performClick()
        assertEquals(listOf("A-1"), chosen)
    }

    @Test
    fun us1_8_hifi_motyw_jasny() {
        compose.setContent { List(query = "") }
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun List(query: String) {
        DostepnyKampusTheme {
            BuildingList(
                catalog = catalog,
                query = query,
                search = BuildingSearch(query, searchBuildings(query, catalog.buildings)),
                areas = areas,
                onBuildingClick = { chosen += it },
            )
        }
    }

    private fun area(name: String, vararg symbols: String) =
        MapArea(name, symbols.toList(), CampusBounds(west = 0.0, south = 0.0, east = 1.0, north = 1.0))

    private fun building(symbol: String): Building {
        val corners = listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 0.0)
        val outline = GeoPolygon(listOf(corners.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        return Building(symbol = symbol, name = "Budynek testowy", outline = outline)
    }
}
