/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.data.building.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.building.ServerEntrancesRepository
import io.github.dostepnykampus.core.data.server.ServerCard
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.EvacuationExit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Wyjścia ewakuacyjne karty V2 od serwera do stanu karty. Wymagania: US2.6, BR-27 */
@OptIn(ExperimentalCoroutinesApi::class)
class CardEvacuationExitsTest {
    private val catalog = BuildingCatalogRepository(EmptyCoroutineContext) { ASSET_JSON }

    @BeforeEach
    fun main() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterEach
    fun reset() {
        Dispatchers.resetMain()
    }

    /**
     * Wiersz: V2 budynku, dla którego serwer podał wyjście ewakuacyjne → wyjście w stanie karty, a otwarcie karty
     *   pyta serwer o kartę budynku raz.
     *
     * Wymagania: US2.6, NFR-10
     */
    @Test
    fun takesServerExitsOnce() =
        runTest {
            var calls = 0
            val server = ServerEntrancesRepository { ServerReply.Ok(DETAILS).also { calls++ } }
            val card =
                BuildingCardViewModel(
                    symbol = "A-1",
                    repository = catalog,
                    networkStatus = { flowOf(true) },
                    serverEntrances = server,
                )
            backgroundScope.launch { card.uiState.collect {} }
            advanceUntilIdle()
            val ready = card.uiState.value as? BuildingCardUiState.Ready
            assertEquals(listOf(EXIT) to 1, ready?.evacuationExits to calls, "wyjścia z jednego zapytania")
        }

    private companion object {
        val EXIT =
            EvacuationExit(
                id = "pwr/9",
                location = GeoPoint(longitude = 0.5, latitude = 0.5),
                description = "Wyjście ewakuacyjne z pomieszczenia 41",
                doorWidthCm = 90,
            )

        val DETAILS by lazy {
            ServerCard(
                building = Building(symbol = "A-1", name = "Gmach", outline = GeoPolygon(emptyList())),
                entrances = emptyList(),
                evacuationExits = listOf(EXIT),
            )
        }

        const val ASSET_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["A-1"],"display_name":"Gmach",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }
}
