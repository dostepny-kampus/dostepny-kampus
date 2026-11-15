/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.geo.building.parseBuildingCatalog
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/** Granice terenów PWr z pliku geometrii. Wymagania: BR-G5 */
class AreaBoundariesTest {
    /** Każdy teren z budynków katalogu ma w pliku granic swoją granicę, a plik nie ma terenów spoza katalogu. */
    @Test
    fun br_g5_every_area_has_boundary() {
        val catalog = parseBuildingCatalog(File(System.getProperty("dostepnykampus.buildings")).readText())
        val boundaries = parseAreaBoundaries(File(System.getProperty("dostepnykampus.areas")).readText())
        assertEquals(campusAreas(catalog.buildings).map { it.name }.toSet(), boundaries.keys)
        assertTrue(boundaries.getValue("Kampus główny") is GeoMultiPolygon, "Kampus główny z kilku części")
    }

    /** Teren „T-9 Atol” z jednym wielokątem: granica po nazwie, punkty w kolejności pliku. */
    @Test
    fun br_g5_boundary_by_name() {
        val geoJson =
            """{"type":"FeatureCollection","features":[{"type":"Feature","properties":{"name":"T-9 Atol"},""" +
                """"geometry":{"type":"Polygon","coordinates":[[[17,51],[17.001,51],[17.001,51.001],[17,51]]]}}]}"""
        val ring = listOf(17.0 to 51.0, 17.001 to 51.0, 17.001 to 51.001, 17.0 to 51.0)
        val expected = GeoPolygon(listOf(ring.map { (lon, lat) -> GeoPoint(longitude = lon, latitude = lat) }))
        assertEquals(mapOf("T-9 Atol" to expected), parseAreaBoundaries(geoJson))
    }
}
