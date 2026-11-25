/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ślad mapy bez włączonego śledzenia: znaczniki niczego nie zmieniają, a zmiana terenu i sekcja oddają wynik
 * bloku albo jego wyjątek. Sekcja GeoJSON nosi stałą nazwę źródła.
 *
 * Wymagania: NFR-57, NFR-56
 */
@RunWith(RobolectricTestRunner::class)
class MapTraceTest {
    @Test
    fun areaChangeReturnsBlockResult() {
        val result = MapTrace.areaChange { "Wittigowo" }

        assertEquals("zmiana terenu oddaje wynik bloku", "Wittigowo", result)
    }

    @Test
    fun markersKeepBlockResult() {
        val result =
            MapTrace.areaChange {
                MapTrace.startStyle(1)
                CameraMotion.entries.forEachIndexed { index, motion ->
                    MapTrace.startMove(motion, index)
                    MapTrace.frame(zoom = 17.5, latitude = 51.1, longitude = 17.06)
                    MapTrace.endMove(motion, index)
                }
                MapTrace.endStyle(1)
                CameraMotion.entries.size
            }

        assertEquals("znaczniki bez śledzenia nie zmieniają wyniku", CameraMotion.entries.size, result)
    }

    @Test
    fun sectionReturnsBlockResult() {
        val result = MapTrace.section(name = MapTrace.CARD_STATE_SECTION) { "C-13" }

        assertEquals("sekcja oddaje wynik bloku", "C-13", result)
    }

    @Test
    fun sectionPassesException() {
        assertThrows("sekcja przepuszcza wyjątek bloku", IllegalStateException::class.java) {
            MapTrace.section(name = MapTrace.SELECTION_SECTION) { error("styl bez warstwy") }
        }
    }

    @Test
    fun namesGeoJsonSection() {
        val name = MapTrace.geoJsonSection("plan")

        assertEquals("nazwa sekcji GeoJSON źródła", "Mapa: GeoJSON plan", name)
    }
}
