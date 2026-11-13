/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.data

import io.github.dostepnykampus.core.model.GeoPoint
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

/**
 * Sąsiedni teren mapy V1 na prostokątach testowych (nie dane PWr): „Wschód” około 2 km i „Daleki wschód” około
 * 6 km na wschód od terenu „Środek”, „Północ” około 3 km na północ.
 */
class AreaNeighborTest {
    private val middle = area("Środek", west = 17.0, south = 51.0)
    private val center = middle.bounds.center
    private val data =
        CampusMapData(
            buildings = JsonObject(emptyMap()),
            labels = emptyList(),
            labelPoints = JsonObject(emptyMap()),
            entrances = JsonObject(emptyMap()),
            start = null,
            areas =
                listOf(
                    area("Daleki wschód", west = 17.09, south = 51.0),
                    middle,
                    area("Wschód", west = 17.03, south = 51.0),
                    area("Północ", west = 17.0, south = 51.03),
                ),
        )

    /** Wiersz: V1, widok na wschodniej granicy: teren w tę stronę z odległością i krawędzią. Wymagania: US1.7 */
    @Test
    fun us1_7_neighbor_beyond_edge() {
        val expected = AreaNeighbor(name = "Wschód", kilometers = 2, edge = MapEdge.EAST)
        assertEquals(expected, data.areaNeighbor(middle, view(longitude = middle.bounds.east)))
        assertEquals(expected, data.areaNeighbor(middle, view(longitude = middle.bounds.east - AFTER_GESTURE)))
    }

    /** Wiersz: V1, widok w środku terenu: bez przycisku sąsiedniego terenu. Wymagania: US1.7 */
    @Test
    fun us1_7_no_neighbor_inside() {
        assertNull(data.areaNeighbor(middle, view()))
    }

    /** Wiersz: V1, widok na granicy, za którą nie ma terenu: bez przycisku sąsiedniego terenu. Wymagania: US1.7 */
    @Test
    fun us1_7_no_neighbor_that_way() {
        assertNull(data.areaNeighbor(middle, view(longitude = middle.bounds.west)))
        assertNull(data.areaNeighbor(middle, view(latitude = middle.bounds.south)))
    }

    /** Wiersz: V1, dwa tereny w tę samą stronę: przycisk prowadzi do bliższego. Wymagania: US1.7 */
    @Test
    fun us1_7_nearest_neighbor_wins() {
        val corner = GeoPoint(longitude = middle.bounds.east, latitude = middle.bounds.north)
        assertEquals("Wschód", data.areaNeighbor(middle, corner)?.name)
        assertEquals(MapEdge.NORTH, data.areaNeighbor(middle, view(latitude = middle.bounds.north))?.edge)
    }

    private fun view(longitude: Double = center.longitude, latitude: Double = center.latitude) =
        GeoPoint(longitude = longitude, latitude = latitude)

    private fun area(name: String, west: Double, south: Double) =
        MapArea(
            name = name,
            symbols = listOf(name),
            bounds = CampusBounds(west = west, south = south, east = west + SIDE, north = south + SIDE),
        )

    private companion object {
        const val SIDE = 0.01
        const val AFTER_GESTURE = 1e-6
    }
}
