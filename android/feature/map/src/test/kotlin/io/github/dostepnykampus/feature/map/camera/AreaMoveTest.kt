/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private val free = FreeMap(widthDp = 411.0, heightDp = 600.0, endDp = 160.0, marginDp = 24.0)
private val start = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.11), zoom = 16.0)

/** Ruch mapy po wyborze terenu na punktach testowych (nie dane PWr). Wymagania: US1.7, NFR-56 */
class AreaMoveTest {
    /** Wiersz: V1, teren ok. 2 km dalej — przelot z czasem z drogi, dłuższy niż 300 ms i najwyżej 1,5 s. */
    @Test
    fun fliesByDistance() {
        val move = areaMove(from = start, to = eastBy(degrees = 0.03), free = free, animationScale = 1f)
        assertEquals(CameraMotion.FLIGHT, move.motion, "daleki teren: przelot")
        assertTrue(move.durationMillis in 301L..1500L, "czas z drogi: ${move.durationMillis} ms")
    }

    /** Wiersz: V1, widok terenu w zasięgu ekranu — przesunięcie w 400 ms. */
    @Test
    fun shiftsToNearArea() {
        val move = areaMove(from = start, to = eastBy(degrees = 0.0005), free = free, animationScale = 1f)
        assertEquals(CameraMotion.EASE, move.motion, "bliski widok: przesunięcie")
        assertEquals(400L, move.durationMillis, "bliski widok: 400 ms")
    }

    /** Wiersz: V1 przy systemowym „Usuń animacje” — od razu stan końcowy. Wymagania: NFR-56 */
    @Test
    fun jumpsWithoutAnimations() {
        val move = areaMove(from = start, to = eastBy(degrees = 0.03), free = free, animationScale = 0f)
        assertEquals(CameraMotion.NONE, move.motion, "bez animacji: skok")
        assertEquals(0L, move.durationMillis, "bez animacji: 0 ms")
    }

    private fun eastBy(degrees: Double): MapCamera =
        start.copy(center = start.center.copy(longitude = start.center.longitude + degrees))
}
