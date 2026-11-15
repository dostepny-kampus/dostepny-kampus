/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.model.building.BuildingCatalog
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kontury z serwera i wejścia z pliku. Przy błędzie sieci albo serwera cały katalog jest z pliku, ale tylko gdy
 * z serwera nic jeszcze nie pobrano; inaczej zostaje katalog pobrany z serwera. Ponowna próba pyta
 * serwer od nowa, a zwykłe czytanie bierze katalog z pamięci.
 *
 * Wymagania: NFR-10
 */
class ServerBuildingCatalogTest {
    private val serverCalls = AtomicInteger()

    @Test
    fun takesOutlinesFromServer() {
        val outline = square(SERVER_SPAN)
        val merged =
            catalog {
                ServerReply.Ok(
                    BuildingList(
                        source = SERVER_SOURCE,
                        buildings =
                            listOf(
                                apiBuilding(symbol = "A-10", name = "Nowy A-10", outline = outline),
                                apiBuilding(symbol = "A-2", name = "Nowy A-2", outline = outline),
                            ),
                    ),
                )
            }
        assertEquals(listOf("A-2", "A-10"), merged.buildings.map { it.symbol })
        assertEquals(listOf("Nowy A-2", "Nowy A-10"), merged.buildings.map { it.name })
        assertEquals(listOf(outline, outline), merged.buildings.map { it.outline })
        assertEquals(listOf("A-2", "A-10"), merged.entrances.map { it.buildingSymbol })
        assertEquals(SERVER_SOURCE, merged.attribution)
        assertEquals(listOf("A-2", "A-10", "C-1"), catalog().entrances.map { it.buildingSymbol })
    }

    /** Wiersz: plan pięter ma tylko budynek szczegółowy z listy serwera; z pliku nie ma żadnego. Wymagania: BR-G1 */
    @Test
    fun marksDetailedFromServer() {
        val outline = square(SERVER_SPAN)
        val buildings =
            listOf(
                apiBuilding(symbol = "A-1", name = "A-1", outline = outline, level = DetailLevel.DETAILED),
                apiBuilding(symbol = "A-2", name = "A-2", outline = outline),
            )
        val server = catalog { ServerReply.Ok(BuildingList(source = SERVER_SOURCE, buildings = buildings)) }

        val offline = catalog { ServerReply.Offline }
        assertEquals(listOf(setOf("A-1"), emptySet()), listOf(server.detailed, offline.detailed))
    }

    @Test
    fun usesAssetOnNetworkError() {
        val failed = catalog { ServerReply.Offline }
        assertEquals(catalog(), failed)
    }

    @Test
    fun usesAssetOnServerError() {
        val failed = catalog { ServerReply.Fault(SERVER_ERROR) }
        assertEquals(catalog(), failed)
    }

    @Test
    fun usesAssetForEmptyList() {
        val empty = catalog { ServerReply.Ok(BuildingList(source = SERVER_SOURCE, buildings = emptyList())) }
        assertEquals(catalog(), empty)
    }

    /**
     * Wiersz: zwykłe otwarcie karty albo mapy → katalog z pamięci, bez drugiego zapytania do serwera.
     *
     * Wymagania: US1, NFR-10
     */
    @Test
    fun asksServerOnce() {
        val repository = repository({ listed("A-2") }, { listed("A-2", "N-1") })
        runBlocking { repeat(2) { repository.catalog() } }
        assertEquals(1, serverCalls.get())
    }

    /**
     * Wiersz: V2, budynku nie ma w telefonie, „Spróbuj ponownie” po połączeniu z internetem → katalog z serwera,
     *   choć pierwsze czytanie poszło z pliku bez sieci.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun retriesAfterOffline() {
        val repository = repository({ OFFLINE }, { listed("A-2", "N-1") })
        runBlocking {
            assertEquals(catalog(), repository.catalog())
            assertEquals(listOf("A-2", "N-1"), repository.refresh().buildings.map { it.symbol })
            assertEquals(listOf("A-2", "N-1"), repository.catalog().buildings.map { it.symbol })
        }
    }

    /** Wiersz: V2, „Spróbuj ponownie” → widać budynek, który serwer dodał po pierwszym czytaniu. Wymagania: NFR-10 */
    @Test
    fun seesNewBuildingOnRetry() {
        val repository = repository({ listed("A-2") }, { listed("A-2", "N-1") })
        runBlocking {
            assertEquals(listOf("A-2"), repository.catalog().buildings.map { it.symbol })
            assertEquals(listOf("A-2", "N-1"), repository.refresh().buildings.map { it.symbol })
        }
        assertEquals(2, serverCalls.get())
    }

    /**
     * Wiersz: „Spróbuj ponownie” bez sieci albo z zerwanym połączeniem → zostają budynki pobrane wcześniej
     *   z serwera, nie sam plik.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun keepsDataOnFailedRetry() {
        val repository =
            repository({ listed("A-2", "N-1") }, { OFFLINE }, { throw IOException("zerwane połączenie") })
        runBlocking {
            val downloaded = repository.catalog()
            assertEquals(downloaded, repository.refresh())
            val afterError =
                try {
                    repository.refresh()
                } catch (expected: IOException) {
                    downloaded
                }
            assertEquals(downloaded, afterError)
            assertEquals(downloaded, repository.catalog())
        }
    }

    /**
     * Wiersz: otwarcie karty i „Spróbuj ponownie” naraz → jedno zapytanie do serwera, dopóki pierwsze trwa.
     *
     * Wymagania: NFR-10
     */
    @Test
    fun waitsForReadOnRetry() {
        val gate = CompletableDeferred<Unit>()
        val server: suspend () -> ServerReply<BuildingList> = {
            serverCalls.incrementAndGet()
            gate.await()
            listed("A-2", "N-1")
        }
        val repository =
            BuildingCatalogRepository(
                buildingCache = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = server,
            ) { assetJson() }
        runBlocking {
            val opened = launch { repository.catalog() }
            val retried = launch { repository.refresh() }
            repeat(PENDING_YIELDS) { yield() }
            assertEquals(1, serverCalls.get())
            gate.complete(Unit)
            opened.join()
            retried.join()
            assertEquals(listOf("A-2", "N-1"), repository.catalog().buildings.map { it.symbol })
        }
    }

    private fun catalog(remote: (suspend () -> ServerReply<BuildingList>)? = null): BuildingCatalog =
        runBlocking {
            BuildingCatalogRepository(
                buildingCache = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = remote,
            ) { assetJson() }.catalog()
        }

    private fun repository(vararg answers: () -> ServerReply<BuildingList>): BuildingCatalogRepository {
        val next = answers.iterator()
        val server: suspend () -> ServerReply<BuildingList> = { next.next()().also { serverCalls.incrementAndGet() } }
        return BuildingCatalogRepository(
            buildingCache = null,
            ioContext = EmptyCoroutineContext,
            listBuildings = server,
        ) { assetJson() }
    }

    private fun listed(vararg symbols: String): ServerReply<BuildingList> =
        ServerReply.Ok(
            BuildingList(
                source = SERVER_SOURCE,
                buildings =
                    symbols.map { symbol ->
                        apiBuilding(symbol = symbol, name = "Serwer $symbol", outline = square(SERVER_SPAN))
                    },
            ),
        )

    private fun apiBuilding(
        symbol: String,
        name: String,
        outline: GeoPolygon,
        level: DetailLevel = DetailLevel.BASIC,
    ): ApiBuilding =
        ApiBuilding(
            building = Building(symbol = symbol, name = name, outline = outline),
            detailLevel = level,
        )

    private fun square(span: Double): GeoPolygon =
        GeoPolygon(
            rings =
                listOf(
                    listOf(0.0 to 0.0, span to 0.0, span to span, 0.0 to 0.0).map { (longitude, latitude) ->
                        GeoPoint(longitude = longitude, latitude = latitude)
                    },
                ),
        )

    private fun assetJson(): String =
        """{"type":"FeatureCollection","attribution":"$ASSET_SOURCE","features":[""" +
            listOf(
                building(symbol = "A-10", name = "Stary A-10"),
                building(symbol = "A-2", name = "Stary A-2"),
                building(symbol = "C-1", name = "Tylko plik"),
                entrance(symbol = "A-2", type = "main"),
                entrance(symbol = "A-2", type = "emergency"),
                entrance(symbol = "A-10", type = "yes"),
                entrance(symbol = "C-1", type = "main"),
            ).joinToString(",") +
            "]}"

    private fun building(symbol: String, name: String): String =
        """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"display_name":"$name",""" +
            """"osm_id":"way/$symbol"},"geometry":$POLYGON}"""

    private fun entrance(symbol: String, type: String): String =
        """{"type":"Feature","properties":{"kind":"entrance","symbols":["$symbol"],"osm_id":"n/$symbol-$type",""" +
            """"entrance":"$type"},"geometry":$POINT}"""

    private companion object {
        const val SERVER_SOURCE = "© serwer"
        const val ASSET_SOURCE = "© OpenStreetMap contributors"
        const val SERVER_SPAN = 2.0
        const val SERVER_ERROR = 500
        const val PENDING_YIELDS = 5
        const val POLYGON = """{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}"""
        const val POINT = """{"type":"Point","coordinates":[0,0]}"""
        val OFFLINE = ServerReply.Offline
    }
}
