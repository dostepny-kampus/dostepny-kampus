/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.geo.parseGeoArea
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Building
import io.github.dostepnykampus.core.network.building.ApiBuilding
import io.github.dostepnykampus.core.network.building.BuildingList
import io.github.dostepnykampus.core.network.building.DetailLevel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Błąd prowadzącego pobranie katalogu dostają też czekający: jedno zapytanie i jeden odczyt pliku dla wszystkich.
 *
 * Wymagania: NFR-10
 */
@OptIn(ExperimentalCoroutinesApi::class)
class BuildingCatalogFailureTest {
    private val reads = AtomicInteger()

    /** Wiersz: błąd serwera u prowadzącego → trzej wywołujący dostają go bez kolejnych zapytań do serwera. */
    @Test
    fun asksServerOnceWhenServerFails() {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val repository =
            BuildingCatalogRepository(
                serverCopy = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = {
                    calls.incrementAndGet()
                    gate.await()
                    throw IOException("network down")
                },
            ) { FILE_JSON }
        val messages =
            runBlocking {
                supervisorScope {
                    val results = parallel { repository.catalog() }
                    gate.complete(Unit)
                    results.joinAll()
                    results.map { it.getCompletionExceptionOrNull()?.message }
                }
            }
        assertEquals(List(WAITERS) { "network down" }, messages, "wywołujący nie dostali tego samego błędu")
        assertEquals(1, calls.get(), "liczba zapytań do serwera")
    }

    /** Wiersz: plik konturów o złym formacie → trzy równoległe wywołania dają jeden odczyt i ten sam błąd. */
    @Test
    fun readsBrokenFileOnce() {
        val held = ArrayDeque<Runnable>()
        val io =
            object : CoroutineDispatcher() {
                override fun dispatch(context: CoroutineContext, block: Runnable) {
                    held.addLast(block)
                }
            }
        val shared = BuildingCatalogRepository(io) { "{".also { reads.incrementAndGet() } }
        val failures =
            runBlocking {
                supervisorScope {
                    val results = parallel { shared.catalog() }
                    while (results.any { it.isActive }) {
                        yield()
                        held.removeFirstOrNull()?.run()
                    }
                    results.joinAll()
                    results.map { it.getCompletionExceptionOrNull() is IllegalArgumentException }
                }
            }
        val expected = List(WAITERS) { true }
        assertEquals(expected, failures, "wywołujący nie dostali tego samego błędu")
        assertEquals(1, reads.get(), "plik czytany raz")
    }

    /** Czekający wznawia pobranie, gdy prowadzący je anuluje. */
    @Test
    fun retriesWhenLeaderIsCancelled() {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        val repository =
            BuildingCatalogRepository(
                serverCopy = null,
                ioContext = EmptyCoroutineContext,
                listBuildings = {
                    if (calls.incrementAndGet() == 1) gate.await()
                    serverList()
                },
            ) { FILE_JSON }
        val catalog =
            runBlocking {
                val leader = launch(start = CoroutineStart.UNDISPATCHED) { repository.catalog() }
                val follower = async(start = CoroutineStart.UNDISPATCHED) { repository.catalog() }
                leader.cancelAndJoin()
                follower.await()
            }
        assertEquals(listOf("C-1"), catalog.buildings.map { it.symbol }, "katalog po ponowieniu")
        assertEquals(2, calls.get(), "po anulowaniu oczekiwano jednego ponowienia")
    }

    private fun CoroutineScope.parallel(call: suspend () -> Any): List<Deferred<Any>> =
        List(WAITERS) { async(start = CoroutineStart.UNDISPATCHED) { call() } }

    private fun serverList(): ServerReply<BuildingList> {
        val outline = parseGeoArea("""{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}""") as GeoPolygon
        val building = ApiBuilding(Building(symbol = "C-1", name = "Serwer C-1", outline = outline), DetailLevel.BASIC)
        return ServerReply.Ok(BuildingList(source = "© serwer", buildings = listOf(building)))
    }

    private companion object {
        const val WAITERS = 3
        const val FILE_JSON =
            """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-1"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}]}"""
    }
}
