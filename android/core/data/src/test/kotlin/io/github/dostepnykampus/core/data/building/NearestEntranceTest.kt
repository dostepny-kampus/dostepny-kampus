/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.building

import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Najbliższe wejście budynku dla punktu zgłoszenia z katalogu budynków.
 *
 * Wymagania: US7.4, US8.4, NFR-19
 */
class NearestEntranceTest {
    private val near = NearestEntrance(BuildingCatalogRepository(EmptyCoroutineContext) { CAMPUS })

    @Test
    fun findsOwnBuildingEntrance() {
        val found = runBlocking { near.of(buildingSymbol = "C-13", point = POINT) }
        assertEquals("n/1", found?.entrance?.id, "wejście budynku C-13")
        assertEquals(0.0, found?.meters ?: -1.0, 1.0, "odległość od wejścia w punkcie")
    }

    @Test
    fun noBuildingNoEntrance() {
        assertNull(runBlocking { near.of(buildingSymbol = null, point = POINT) }, "zgłoszenie poza budynkiem")
        assertNull(runBlocking { near.of(buildingSymbol = "A-1", point = POINT) }, "budynek bez wejść")
    }

    @Test
    fun missingCatalogGivesNull() {
        val broken = NearestEntrance(BuildingCatalogRepository(EmptyCoroutineContext) { throw IOException("brak") })
        assertNull(runBlocking { broken.of(buildingSymbol = "C-13", point = POINT) }, "bez katalogu")
    }

    private companion object {
        val POINT = GeoPoint(longitude = 17.06, latitude = 51.1)
        const val CAMPUS =
            """{"type":"FeatureCollection","attribution":"© plik","features":[""" +
                """{"type":"Feature","properties":{"kind":"building","symbols":["C-13"],"osm_id":"way/1"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17,51],[18,51],[18,52],[17,51]]]}},""" +
                """{"type":"Feature","properties":{"kind":"entrance","symbols":["C-13"],"osm_id":"n/1",""" +
                """"ref":"Wejście A"},"geometry":{"type":"Point","coordinates":[17.06,51.1]}}]}"""
    }
}
