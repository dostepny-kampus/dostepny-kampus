/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Lista budynków podstawowych z publicznego konstruktora daje ten sam katalog co lista klienta HTTP
 * z budynkami na poziomie [DetailLevel.BASIC].
 *
 * Wymagania: NFR-10
 */
class BasicServerListTest {
    @Test
    fun nfr10_ok_is_basic_list() {
        assertEquals(
            direct(ServerReply.Ok(BuildingList(SOURCE, listOf(ApiBuilding(SERVER, DetailLevel.BASIC))))),
            fixture(ServerReply.Ok(ServerBuildings(SOURCE, listOf(SERVER)))),
        )
    }

    @Test
    fun nfr10_offline_is_offline() {
        assertEquals(direct(ServerReply.Offline), fixture(ServerReply.Offline))
    }

    @Test
    fun nfr10_fault_is_fault() {
        assertEquals(direct(ServerReply.Fault(SERVER_ERROR)), fixture(ServerReply.Fault(SERVER_ERROR)))
    }

    @Test
    fun nfr10_missing_is_missing() {
        assertEquals(direct(ServerReply.Missing), fixture(ServerReply.Missing))
    }

    private fun direct(result: ServerReply<BuildingList>): BuildingCatalog =
        runBlocking {
            BuildingCatalogRepository(
                buildingCache = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = { result },
            ) { ASSET }.catalog()
        }

    private fun fixture(reply: ServerReply<ServerBuildings>): BuildingCatalog =
        runBlocking { BuildingCatalogRepository(EmptyCoroutineContext, listBuildings = { reply }) { ASSET }.catalog() }

    private companion object {
        val ORIGIN = GeoPoint(longitude = 0.0, latitude = 0.0)

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
                            listOf(
                                ORIGIN,
                                GeoPoint(longitude = 0.0, latitude = 2.0),
                                GeoPoint(longitude = 2.0, latitude = 2.0),
                                ORIGIN,
                            ),
                        ),
                    ),
            )
    }
}
