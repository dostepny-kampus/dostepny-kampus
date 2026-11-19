/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerBuildings
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
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
    fun mapsOkToBasicList() {
        assertEquals(
            direct(ServerReply.Ok(BuildingList(SOURCE, listOf(ApiBuilding(SERVER, DetailLevel.BASIC))))),
            fixture(ServerReply.Ok(ServerBuildings(SOURCE, listOf(SERVER)))),
            "odpowiedź z listą jak z bezpośredniego klienta",
        )
    }

    @Test
    fun mapsOfflineToOffline() {
        assertEquals(direct(ServerReply.Offline), fixture(ServerReply.Offline), "offline jak z bezpośredniego klienta")
    }

    @Test
    fun mapsFaultToFault() {
        assertEquals(
            direct(ServerReply.Fault(SERVER_ERROR)),
            fixture(ServerReply.Fault(SERVER_ERROR)),
            "błąd serwera jak z bezpośredniego klienta",
        )
    }

    @Test
    fun mapsMissingToMissing() {
        assertEquals(
            direct(ServerReply.Missing),
            fixture(ServerReply.Missing),
            "brak zasobu jak z bezpośredniego klienta",
        )
    }

    private fun direct(result: ServerReply<BuildingList>): BuildingCatalog =
        runBlocking {
            BuildingCatalogRepository(
                buildingCache = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = { result },
            ) { ASSET }.catalog()
        }

    @Test
    fun mapsDetailedSymbols() {
        val catalog = fixture(ServerReply.Ok(ServerBuildings(SOURCE, listOf(SERVER), detailed = setOf(SYMBOL))))

        assertEquals(setOf(SYMBOL), catalog.detailed, "symbol szczegółowy z listy serwera")
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
