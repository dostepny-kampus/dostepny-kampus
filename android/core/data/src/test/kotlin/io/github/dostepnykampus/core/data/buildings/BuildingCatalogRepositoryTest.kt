/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.EmptyCoroutineContext

/** Katalog budynków z repozytorium na małym syntetycznym pliku w formacie `data/buildings` (nie dane PWr). */
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
    }
}
