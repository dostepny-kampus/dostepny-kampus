/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.buildings

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/** Katalog budynków z repozytorium na małym syntetycznym pliku w formacie `data/buildings` (nie dane PWr). */
class BuildingCatalogRepositoryTest {
    private var reads = 0
    private val repository =
        BuildingCatalogRepository(EmptyCoroutineContext) {
            reads++
            collection(
                listOf("A-10", "B-1", "A-2", "D-8a", "A-1").map(::building) +
                    listOf("main", "emergency", "service", "exit", "staircase", "garage", "yes").map(::entrance),
            )
        }

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
        assertEquals(1, reads)
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
}
