/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.model.Building
import io.github.dostepnykampus.core.model.BuildingCatalog
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.network.ApiBuilding
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.BuildingList
import io.github.dostepnykampus.core.network.DetailLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/** Strumień katalogu dla mapy równolegle z `catalog()` i `refresh()` na wstrzymanym serwerze (NFR-10, NFR-11, W2). */
class MapStreamConcurrencyTest {
    private val calls = AtomicInteger()
    private val gate = CompletableDeferred<Unit>()
    private var isFirstCallFailing = false
    private val repository = BuildingCatalogRepository(EmptyCoroutineContext, ::server) { FILE_JSON }

    /** Wiersz: mapa zbiera katalog, a inny odbiorca woła catalog() → jedno zapytanie do serwera, NFR-10. */
    @Test
    fun nfr10_map_shares_request() {
        inRun {
            val map = launch { repository.catalogUpdates().collect {} }
            val card = launch { repository.catalog() }
            settle()
            assertEquals(1, calls.get(), "drugie zapytanie do serwera")
            gate.complete(Unit)
            map.join()
            card.join()
        }
    }

    /**
     * Wiersz: niepustą listę przyjmuje refresh(), a zapytanie mapy pada → ostatni stan mapy to katalog z serwera, W2.
     */
    @Test
    fun nfr10_map_sees_other_answer() {
        inRun {
            isFirstCallFailing = true
            val seen = mutableListOf<BuildingCatalog>()
            val map = launch { repository.catalogUpdates().collect { seen += it } }
            settle()
            val retry = launch { repository.refresh() }
            settle()
            gate.complete(Unit)
            retry.join()
            withTimeoutOrNull(WAIT_MS) { map.join() }
            map.cancel()
            assertEquals(listOf("S-1"), seen.last().buildings.map { it.symbol }, "mapa została na pliku")
        }
    }

    /**
     * Wiersz: zapytanie mapy pada, a potem, przy wciąż zbieranym strumieniu, osobny refresh() przyjmuje niepustą
     *   listę → ostatni stan mapy to katalog z serwera, dwa zapytania, W2.
     */
    @Test
    fun nfr10_map_sees_late_refresh() {
        val offline = ApiResult.NetworkError(IOException("brak"))
        val answers = ArrayDeque<ApiResult<BuildingList>>(listOf(offline, LIST))
        val ask: suspend () -> ApiResult<BuildingList> = {
            calls.incrementAndGet()
            answers.removeFirst()
        }
        val late = BuildingCatalogRepository(EmptyCoroutineContext, ask) { FILE_JSON }
        inRun {
            val seen = mutableListOf<BuildingCatalog>()
            val map = launch { late.catalogUpdates().collect { seen += it } }
            settle()
            late.refresh()
            withTimeoutOrNull(WAIT_MS) { map.join() }
            map.cancel()
            assertEquals(listOf("S-1"), seen.last().buildings.map { it.symbol }, "mapa została na pliku")
            assertEquals(2, calls.get(), "liczba zapytań do serwera")
        }
    }

    /** Wiersz: refresh() w toku na wstrzymanym serwerze → pierwsza emisja mapy to plik w ≤ 1 s, W1, NFR-11. */
    @Test
    fun nfr11_file_while_retry_runs() {
        inRun {
            launch { repository.refresh() }
            settle()
            try {
                val first = withTimeoutOrNull(WAIT_MS) { repository.catalogUpdates().first() }
                val symbols = first?.buildings.orEmpty().map { it.symbol }
                assertEquals(listOf("C-1"), symbols, "mapa czeka na trwające pobranie")
            } finally {
                gate.complete(Unit)
            }
        }
    }

    private suspend fun server(): ApiResult<BuildingList> {
        val mine = calls.incrementAndGet()
        gate.await()
        val isFailing = isFirstCallFailing && mine == 1 && calls.get() > 1
        return if (isFailing) ApiResult.NetworkError(IOException("brak")) else LIST
    }

    private suspend fun settle() = repeat(YIELDS) { yield() }

    private fun inRun(body: suspend CoroutineScope.() -> Unit) = runBlocking { body() }

    private companion object {
        const val WAIT_MS = 1_000L
        const val YIELDS = 5
        const val FILE_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-1"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
        val LIST =
            ApiResult.Success(
                BuildingList(
                    source = "© serwer",
                    buildings =
                        listOf(
                            ApiBuilding(
                                building = Building(symbol = "S-1", name = "Serwer", outline = GeoPolygon(emptyList())),
                                detailLevel = DetailLevel.BASIC,
                            ),
                        ),
                ),
            )
    }
}
