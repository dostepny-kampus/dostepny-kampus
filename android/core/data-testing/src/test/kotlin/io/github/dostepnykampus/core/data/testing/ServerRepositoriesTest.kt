/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.testing

import io.github.dostepnykampus.core.data.buildings.BuildingCatalogRepository
import io.github.dostepnykampus.core.data.buildings.ServerEntrancesRepository
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.Entrance
import io.github.dostepnykampus.core.model.EntranceMarking
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingDetails
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Odpowiedź z testu modułu funkcji daje to samo repozytorium co odpowiadający jej wynik klienta HTTP.
 *
 * Wymagania: NFR-10
 */
class ServerRepositoriesTest {
    @Test
    fun nfr10_ok_is_success() {
        assertEquals(
            direct(ApiResult.Success(BuildingList(SOURCE, listOf(ApiBuilding(SERVER, DetailLevel.BASIC))))),
            fixture(ServerReply.Ok(ServerBuildings(SOURCE, listOf(SERVER)))),
        )
    }

    @Test
    fun nfr10_offline_is_network_error() {
        assertEquals(direct(ApiResult.NetworkError(IOException("brak sieci"))), fixture(ServerReply.Offline))
    }

    @Test
    fun nfr10_fault_is_server_error() {
        assertEquals(direct(ApiResult.ServerError(SERVER_ERROR)), fixture(ServerReply.Fault(SERVER_ERROR)))
    }

    @Test
    fun nfr10_card_keeps_entrances() {
        val replies = ArrayDeque(listOf(ServerReply.Ok(ServerCard(SERVER, listOf(ENTRANCE))), ServerReply.Offline))
        val fixture = serverEntrances { replies.removeFirst() }
        val answers =
            ArrayDeque(
                listOf(
                    ApiResult.Success(BuildingDetails(SERVER, DetailLevel.BASIC, listOf(ENTRANCE))),
                    ApiResult.NetworkError(IOException("brak sieci")),
                ),
            )
        val direct = ServerEntrancesRepository { answers.removeFirst() }
        val expected = runBlocking { List(2) { direct.entrances(SYMBOL) } }
        assertEquals(expected, runBlocking { List(2) { fixture.entrances(SYMBOL) } })
    }

    private fun direct(result: ApiResult<BuildingList>): BuildingCatalog =
        runBlocking {
            BuildingCatalogRepository(EmptyCoroutineContext, listBuildings = { result }) { ASSET }.catalog()
        }

    private fun fixture(reply: ServerReply<ServerBuildings>): BuildingCatalog =
        runBlocking { serverBuildingCatalog(EmptyCoroutineContext, list = { reply }) { ASSET }.catalog() }

    private companion object {
        const val SYMBOL = "A-1"
        const val SOURCE = "© serwer"
        const val SERVER_ERROR = 500
        const val ASSET =
            """{"type":"FeatureCollection","attribution":"© plik","features":[{"type":"Feature",""" +
                """"properties":{"kind":"building","symbols":["A-1"],"display_name":"Z pliku",""" +
                """"osm_id":"way/1"},"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        val SERVER =
            Building(
                symbol = SYMBOL,
                name = "Z serwera",
                outline =
                    GeoPolygon(
                        listOf(
                            listOf(GeoPoint(0.0, 0.0), GeoPoint(0.0, 2.0), GeoPoint(2.0, 2.0), GeoPoint(0.0, 0.0)),
                        ),
                    ),
            )
        val ENTRANCE =
            Entrance(
                id = "node/1",
                buildingSymbol = SYMBOL,
                name = null,
                isMain = true,
                type = null,
                location = GeoPoint(0.0, 0.0),
                marking = EntranceMarking.NO_DATA,
            )
    }
}
