/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.geo

import io.github.dostepnykampus.core.model.GeoArea
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * Kontur budynku jako pole JSON: odczyt, zapis i zła geometria, na kształtach testowych.
 *
 * Wymagania: US1
 */
class GeoAreaJsonTest {
    @Test
    fun readsPolygonFromField() {
        val area = Json.decodeFromString(GeoAreaJson, """{"type":"Polygon","coordinates":[$SQUARE]}""")
        assertEquals(GeoPolygon(listOf(square())), area)
    }

    @Test
    fun roundTripsMultipolygon() {
        val area: GeoArea = GeoMultiPolygon(listOf(GeoPolygon(listOf(square())), GeoPolygon(listOf(square()))))
        val text = Json.encodeToString(GeoAreaJson, area)
        assertEquals(area, Json.decodeFromString(GeoAreaJson, text))
    }

    @Test
    fun rejectsPointAsOutline() {
        assertThrows<IllegalArgumentException> {
            Json.decodeFromString(GeoAreaJson, """{"type":"Point","coordinates":[17.0,51.0]}""")
        }
    }

    private fun square(): List<GeoPoint> =
        listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0)
            .map { (longitude, latitude) -> GeoPoint(longitude = longitude, latitude = latitude) }

    private companion object {
        const val SQUARE = "[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0],[0.0,0.0]]"
    }
}
