/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

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
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.dostepnykampus.core.designsystem.DostepnyKampusTheme
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.CampusSearch
import io.github.dostepnykampus.feature.map.area.AreaOrder
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.building.MissingRoom
import io.github.dostepnykampus.feature.map.building.isRoomNumberOnly
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Wyniki sal pod polem szukaj V1 na salach testowych budynku „A-1” (nie dane PWr): serwer podaje sale, a sam
 * numer bez budynku daje prośbę o budynek.
 *
 * Wymagania: US18.1, NFR-17, NFR-18
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xhdpi")
class RoomResultsTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val chosen = mutableListOf<FoundRoom>()
    private val opened = mutableListOf<String>()

    /**
     * Wiersz: V1, wyniki sal stoją w kolejności z serwera, a każdy ma symbol budynku, numer, rodzaj i piętro.
     * Scenariusz: szukanie-sali.feature: Symbol budynku i cyfra podpowiadają sale tego budynku
     */
    @Test
    fun listsRoomsInServerOrder() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 3")
        val tops =
            ROOMS.map { found ->
                val row = compose.onNodeWithContentDescription(label = "Sala ${found.number},", substring = true)
                row.fetchSemanticsNode().boundsInRoot.top
            }
        assertEquals("Kolejność sal inna niż z serwera", tops.sorted(), tops)
        compose.onNodeWithContentDescription("Sala 329, budynek A-1, sala wykładowa, 2 piętro").assertExists()
        compose.onNodeWithText("Sala wykładowa · 2\u00A0piętro", useUnmergedTree = true).assertExists()
    }

    /** Wiersz: V1, rodzaju „Inne” wynik nie ma, zostaje samo piętro. Wymagania: US18.1 */
    @Test
    fun skipsOtherFunction() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 3")
        compose.onNodeWithContentDescription("Sala 301, budynek A-1, 2 piętro").assertExists()
        compose.onNodeWithText("2\u00A0piętro", useUnmergedTree = true).assertExists()
    }

    /** Wiersz: V1, dotknięcie wyniku sali wybiera tę salę. Wymagania: US18.1, NFR-17 */
    @Test
    fun choosesTappedRoom() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 3")
        compose.onNodeWithContentDescription("Sala 314, budynek A-1, pracownia komputerowa, 2 piętro").performClick()
        assertEquals("Wybrana inna sala", listOf("314"), chosen.map { it.room.number })
    }

    /**
     * Wiersz: V1, sam numer sali daje prośbę o budynek zamiast „Nie znaleźliśmy budynku”.
     * Scenariusz: szukanie-sali.feature: Sam numer sali bez budynku prosi o dopisanie budynku
     */
    @Test
    fun asksForBuildingSymbol() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("329")
        compose.onNodeWithText("Dopisz budynek do numeru sali. Na przykład: A-1 329.").assertExists()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
    }

    /**
     * Wiersz: V1, sali, której nie ma w danych budynku, zdanie o sali i wiersz budynku, bez „Nie znaleźliśmy”.
     * Scenariusz (przykład: dane PWr budynku A-1 nie mają sali 999): szukanie-sali.feature:
     *   Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
     */
    @Test
    fun namesMissingRoomWithBuilding() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 999")
        compose.onNodeWithText("W danych PWr budynku A-1 nie ma sali 999.").assertExists()
        compose.onNodeWithText("Budynek A-1", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Nie znaleźliśmy", substring = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("A-1").performClick()
        assertEquals("Wiersz budynku nie otworzył budynku A-1", listOf("A-1"), opened)
    }

    @Test
    fun matchesLightMockup() {
        compose.setContent { Search() }
        compose.onNode(hasSetTextAction()).performTextInput("A-1 3")
        compose.onRoot().captureRoboImage()
    }

    @Composable
    private fun Search() {
        var search by remember { mutableStateOf(BuildingSearch()) }
        DostepnyKampusTheme {
            CampusSearch(
                catalog = BuildingCatalog(buildings = emptyList(), entrances = emptyList(), attribution = ""),
                search = search,
                onSearch = { query -> search = searchFor(query) },
                isOnline = true,
                syncedAt = null,
                isList = false,
                areas = AreaOrder(emptyList()),
                onListChange = {},
                onBuildingClick = { opened += it },
                onRoomClick = { chosen += it },
            )
        }
    }

    private companion object {
        val SECOND = Floor(id = "a1-2", label = "2 piętro", order = 2)
        val BUILDING_A1 =
            Building(
                symbol = "A-1",
                name = null,
                outline = parseGeoArea("""{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}"""),
            )
        val ROOMS =
            listOf("301" to "Inne", "314" to "Pracownia komputerowa", "329" to "Sala wykładowa").map { (number, kind) ->
                FoundRoom(building = "A-1", floor = SECOND, room = room(number = number, function = kind))
            }

        val FoundRoom.number: String get() = room.number.orEmpty()

        fun searchFor(query: String): BuildingSearch {
            val isMissing = isRoomNumberOnly(query)
            val rooms = if (isMissing || query.isBlank()) emptyList() else ROOMS
            val missing = MissingRoom(building = BUILDING_A1, number = "999").takeIf { query == "A-1 999" }
            return BuildingSearch(
                query = query,
                buildings = emptyList(),
                rooms = rooms,
                isBuildingMissing = isMissing,
                missingRoom = missing,
            )
        }

        fun room(number: String, function: String): Room {
            val corner = GeoPoint(longitude = 17.06, latitude = 51.11)
            return Room(
                id = "a1-$number",
                number = number,
                function = function,
                category = RoomCategory.CLASSROOM,
                geometry = GeoPolygon(listOf(listOf(corner, corner, corner, corner))),
                labelPoint = corner,
                isLabelled = true,
            )
        }
    }
}
