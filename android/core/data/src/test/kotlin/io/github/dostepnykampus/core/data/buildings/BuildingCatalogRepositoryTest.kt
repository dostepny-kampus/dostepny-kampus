/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import io.github.dostepnykampus.core.data.di.DataModule
import io.github.dostepnykampus.core.model.AssessmentSource.WALKTHROUGH
import io.github.dostepnykampus.core.model.EntranceAssessment
import io.github.dostepnykampus.core.network.BuildingsApi
import io.github.dostepnykampus.core.network.NetworkModule
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.File
import java.time.LocalDate
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/** Katalog budynków na syntetycznym pliku `data/buildings` (nie dane PWr) i wejścia karty z serwera. */
class BuildingCatalogRepositoryTest {
    private val reads = AtomicInteger()
    private val json =
        collection(
            listOf("A-10", "B-1", "A-2", "D-8a", "A-1").map(::building) +
                listOf("main", "emergency", "service", "exit", "staircase", "garage", "yes").map(::entrance),
        )
    private val repository = BuildingCatalogRepository(EmptyCoroutineContext) { json.also { reads.incrementAndGet() } }

    @Test
    fun us1_natural_symbol_order() {
        val symbols = runBlocking { repository.catalog() }.buildings.map { it.symbol }
        assertEquals(listOf("A-1", "A-2", "A-10", "B-1", "D-8a"), symbols)
    }

    @Test
    fun br_25_visitor_entrances_only() {
        val types = runBlocking { repository.catalog() }.entrances.map { it.type }
        assertEquals(listOf("main", "service", "staircase", "yes"), types)
    }

    @Test
    fun us1_file_read_once() {
        val first = runBlocking { repository.catalog() }
        assertSame(first, runBlocking { repository.catalog() })
        assertEquals(1, reads.get())
    }

    @Test
    fun us1_parallel_calls_read_once() {
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { pool ->
            val slow =
                BuildingCatalogRepository(pool) {
                    Thread.sleep(SLOW_READ_MS)
                    json.also { reads.incrementAndGet() }
                }
            val (first, second) = runBlocking(pool) { List(2) { async { slow.catalog() } }.awaitAll() }
            assertSame(first, second)
            assertEquals(1, reads.get())
        }
    }

    @Test
    fun us1_real_file_for_visitors() {
        val file = File(requireNotNull(System.getProperty("dostepnykampus.buildings")) { "Test uruchamia Gradle" })
        val real = BuildingCatalogRepository(EmptyCoroutineContext) { file.readText() }
        val catalog = runBlocking { real.catalog() }
        assertEquals(95, catalog.buildings.size)
        assertEquals(70, catalog.entrances.size)
        val visitorTypes = setOf("main", "yes", "service", "staircase", "secondary")
        assertEquals(visitorTypes, catalog.entrances.mapNotNull { it.type }.toSet())
    }

    // Wiersz: wejścia karty w aplikacji pochodzą z `GET /buildings/{symbol}` z dniem i źródłem oznaczenia, US17.3.
    @Test
    fun us17_3_app_asks_server() {
        val found =
            MockWebServer().use { server ->
                server.enqueue(MockResponse().setBody(CARD))
                val api = BuildingsApi(NetworkModule.okHttp(), NetworkModule.json(), server.url("/").toString())
                runBlocking { DataModule.cardEntrances(api).entrances("A-1") }
            }
        assertEquals(listOf(EntranceAssessment(LocalDate.of(2026, 10, 1), WALKTHROUGH)), found.map { it.assessment })
    }

    private fun collection(features: List<String>): String =
        """{"type":"FeatureCollection","attribution":"© OpenStreetMap contributors",""" +
            """"features":[${features.joinToString(",")}]}"""

    private fun building(symbol: String): String =
        """{"type":"Feature","properties":{"kind":"building","symbols":["$symbol"],"osm_id":"way/$symbol"},""" +
            """"geometry":{"type":"Polygon","coordinates":[[[0,0],[1,0],[1,1],[0,0]]]}}"""

    private fun entrance(type: String): String =
        """{"type":"Feature","properties":{"kind":"entrance","symbols":["A-1"],"osm_id":"n/$type",""" +
            """"entrance":"$type"},"geometry":{"type":"Point","coordinates":[0,0]}}"""

    private companion object {
        const val SLOW_READ_MS = 50L
        const val CARD =
            """{"symbol":"A-1","name":"Gmach","detailLevel":"BASIC","outline":{"type":"Polygon","coordinates":""" +
                """[[[0,0],[1,0],[1,1],[0,0]]]},"entrances":[{"id":"a1","buildingSymbol":"A-1","main":true,""" +
                """"location":{"type":"Point","coordinates":[0.5,0]},"marking":"ACCESSIBLE",""" +
                """"markingDate":"2026-10-01","markingSource":"WALKTHROUGH"}]}"""
    }
}
