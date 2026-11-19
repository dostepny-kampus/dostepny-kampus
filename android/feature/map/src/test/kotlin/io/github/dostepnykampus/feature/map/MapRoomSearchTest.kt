/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.map.MapListChoice
import io.github.dostepnykampus.core.data.room.RoomSearch
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.feature.map.building.BuildingSearch
import io.github.dostepnykampus.feature.map.campus.campusMapData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Wyszukiwanie sali w modelu mapy V1, gdy budynku o takim symbolu nie ma. Wymagania: US18.1 */
@OptIn(ExperimentalCoroutinesApi::class)
class MapRoomSearchTest {
    private val asked = mutableListOf<String>()
    private val choice =
        object : MapListChoice {
            override val savedChoice = MutableStateFlow<Boolean?>(null)

            override suspend fun choose(isList: Boolean) = Unit
        }
    private val geoJson =
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
            """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""

    @BeforeEach
    fun main() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    @Test
    fun findsRoomWhenNoBuilding() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 329", reply = ServerReply.Ok(listOf(FOUND)))

            assertEquals(listOf(FOUND), search.rooms, "Model nie oddał sali z serwera")
            assertEquals(listOf("A-1 329"), asked, "Model nie zapytał serwera o salę")
        }

    @Test
    fun skipsRoomSearchForBuilding() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "C-13", reply = ServerReply.Ok(listOf(FOUND)))

            assertEquals(listOf("C-13"), search.buildings.orEmpty().map { it.symbol }, "Budynek zniknął z wyniku")
            assertEquals(emptyList<String>(), asked, "Model pytał o salę, choć budynek jest")
        }

    @Test
    fun givesNoRoomsOffline() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 329", reply = ServerReply.Offline)

            assertEquals(emptyList<FoundRoom>(), search.rooms, "Brak sieci dał sale")
        }

    private suspend fun searched(scope: TestScope, query: String, reply: ServerReply<List<FoundRoom>>): BuildingSearch {
        val viewModel =
            CampusMapViewModel(
                repository = BuildingCatalogRepository(EmptyCoroutineContext) { geoJson },
                networkStatus = { MutableStateFlow(true) },
                mapListChoice = choice,
                computeContext = EmptyCoroutineContext,
                geometry = ::campusMapData,
                server =
                    MapServer(
                        rooms =
                            RoomSearch { text ->
                                asked += text
                                reply
                            },
                    ),
            )
        scope.backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.search(query)
        return viewModel.uiState.first { it.search.query == query && it.search.buildings != null }.search
    }

    private companion object {
        val FOUND =
            FoundRoom(
                building = "A-1",
                floor = Floor(id = "K1.A1.P3", label = "3 piętro", order = 3),
                room =
                    Room(
                        id = "12",
                        number = "329",
                        function = "Sala ćwiczeniowa",
                        category = RoomCategory.LECTURE_HALL,
                        geometry = GeoMultiPolygon(polygons = emptyList()),
                        labelPoint = GeoPoint(longitude = 17.059, latitude = 51.108),
                        isLabelled = false,
                    ),
            )
    }
}
