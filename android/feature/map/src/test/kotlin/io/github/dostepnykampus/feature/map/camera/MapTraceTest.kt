/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Ślad mapy bez włączonego śledzenia: znaczniki niczego nie zmieniają, a zmiana terenu oddaje wynik bloku.
 *
 * Wymagania: NFR-57
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
}
