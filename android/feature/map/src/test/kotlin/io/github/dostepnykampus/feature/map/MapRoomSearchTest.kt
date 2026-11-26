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
import io.github.dostepnykampus.feature.map.campus.NO_SERVER
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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Wyszukiwanie sali w modelu mapy V1: sala, gdy budynku o takim symbolu nie ma, i sam numer. Wymagania: US18.1 */
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
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}},""" +
            """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"osm_id":"way/2"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[2,0],[3,0],[3,1],[2,0]]]}}]}"""

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
    fun findsRoomNumberFirst() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "329 A-1", reply = ServerReply.Ok(listOf(FOUND)))

            assertEquals(listOf(FOUND), search.rooms, "Model nie oddał sali dla numeru przed budynkiem")
            assertEquals(listOf("329 A-1"), asked, "Model nie zapytał serwera o numer przed budynkiem")
        }

    @Test
    fun asksForBuilding() =
        runTest(UnconfinedTestDispatcher()) {
            listOf("329", " sala 329 ", "s. 2.21").forEach { query ->
                val search = searched(scope = this, query = query, reply = ServerReply.Ok(listOf(FOUND)))

                assertTrue(search.isBuildingMissing, "Sam numer sali „$query” nie dał prośby o budynek")
            }
            assertEquals(emptyList<String>(), asked, "Model szukał sali bez budynku")
        }

    @Test
    fun keepsBuildingWithNumber() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 329", reply = ServerReply.Ok(listOf(FOUND)))

            assertFalse(search.isBuildingMissing, "Zapytanie z budynkiem dało prośbę o budynek")
        }

    @Test
    fun givesNoRoomsOffline() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 329", reply = ServerReply.Offline)

            assertEquals(emptyList<FoundRoom>(), search.rooms, "Brak sieci dał sale")
        }

    /**
     * Wiersz: budynek A-1 jest w danych, a serwer nie ma sali 999, więc model podaje brak tej sali w budynku.
     * Scenariusz (przykład: dane PWr budynku A-1 nie mają sali 999): szukanie-sali.feature:
     *   Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
     */
    @Test
    fun namesMissingRoom() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 999", reply = ServerReply.Ok(emptyList()))

            assertEquals("A-1", search.missingRoom?.run { building.symbol }, "Model nie podał budynku bez sali 999")
            assertEquals("999", search.missingRoom?.number, "Model nie podał numeru sali, której nie ma w danych")
        }

    @Test
    fun givesNoMissingRoomOffline() =
        runTest(UnconfinedTestDispatcher()) {
            val search = searched(scope = this, query = "A-1 999", reply = ServerReply.Offline)

            assertNull(search.missingRoom, "Brak sieci dał brak sali w danych")
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
                    NO_SERVER.copy(
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
