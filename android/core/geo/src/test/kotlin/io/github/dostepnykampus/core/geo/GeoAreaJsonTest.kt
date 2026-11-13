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

/** Kontur budynku jako pole JSON: odczyt, zapis i zła geometria, na kształtach testowych. */
class GeoAreaJsonTest {
    private val json = Json

    @Test
    fun us1_polygon_from_field() {
        val area = json.decodeFromString(GeoAreaJson, """{"type":"Polygon","coordinates":[$SQUARE]}""")
        assertEquals(GeoPolygon(listOf(square())), area)
    }

    @Test
    fun us1_multipolygon_round_trip() {
        val area: GeoArea = GeoMultiPolygon(listOf(GeoPolygon(listOf(square())), GeoPolygon(listOf(square()))))
        val text = json.encodeToString(GeoAreaJson, area)
        assertEquals(area, json.decodeFromString(GeoAreaJson, text))
    }

    @Test
    fun us1_point_is_not_outline() {
        assertThrows<IllegalArgumentException> {
            json.decodeFromString(GeoAreaJson, """{"type":"Point","coordinates":[17.0,51.0]}""")
        }
    }

    private fun square(): List<GeoPoint> =
        listOf(0.0 to 0.0, 1.0 to 0.0, 1.0 to 1.0, 0.0 to 1.0, 0.0 to 0.0)
            .map { (longitude, latitude) -> GeoPoint(longitude = longitude, latitude = latitude) }

    private companion object {
        const val SQUARE = "[[0.0,0.0],[1.0,0.0],[1.0,1.0],[0.0,1.0],[0.0,0.0]]"
    }
}
