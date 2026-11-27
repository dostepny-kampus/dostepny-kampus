/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapLimits
import io.github.dostepnykampus.feature.map.camera.PanDirection
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.isSameView
import io.github.dostepnykampus.feature.map.camera.moved
import io.github.dostepnykampus.feature.map.camera.zoomedIn
import io.github.dostepnykampus.feature.map.camera.zoomedOut
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** Przyciski powiększania i przesuwania mapy bez gestów na punkcie testowym, nie na danych PWr. Wymagania: NFR-53 */
class MapCameraTest {
    private val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)

    /**
     * Widok zgłoszony po geście wraca z rachunkiem zmiennoprzecinkowym jako ten sam widok, więc mapa nie leci do
     * miejsca, w którym stoi; przesunięcie o ok. 1 m albo zmiana powiększenia to już inny widok.
     *
     * Wymagania: NFR-56
     */
    @Test
    fun recognisesSameView() {
        val reported = camera.copy(center = GeoPoint(longitude = 17.0 + 1e-12, latitude = 60.0 - 1e-12))
        val moved = camera.copy(center = GeoPoint(longitude = 17.00001, latitude = 60.0))
        assertTrue(camera.isSameView(reported), "ten sam widok po zapisie")
        assertFalse(camera.isSameView(moved), "przesunięcie o ok. 1 m")
        assertFalse(camera.isSameView(camera.zoomedIn()), "inne powiększenie")
    }

    @Test
    fun zoomsOneLevel() {
        assertEquals(START_ZOOM + 1, camera.zoomedIn().zoom)
        assertEquals(START_ZOOM - 1, camera.zoomedOut().zoom)
        assertEquals(camera.center, camera.zoomedIn().center)
    }

    /**
     * Wiersz: V1, „Pomniejsz” oddala tak samo jak gest, do najmniejszego powiększenia, przy którym cały obszar
     * kampusu mieści się w widoku, także poniżej dawnego powiększenia 12.
     *
     * Wymagania: NFR-39, NFR-53
     */
    @Test
    fun keepsZoomInRange() {
        val campus = CampusBounds(west = 16.9, south = 59.95, east = 17.1, north = 60.05)
        val limits = MapLimits(campus, widthDp = 411.0, heightDp = 800.0)
        val closest = generateSequence(camera) { it.zoomedIn() }.elementAt(index = 10)
        val farthest = generateSequence(camera) { limits.clamp(it.zoomedOut()) }.elementAt(index = 10)
        assertEquals(20.0, closest.zoom)
        assertEquals(limits.minZoom, farthest.zoom)
    }

    @Test
    fun movesCenterWithArrows() {
        val step = 70.0 / 131_072.0
        assertEquals(17.0 + step, camera.moved(PanDirection.RIGHT).center.longitude, DELTA)
        assertEquals(17.0 - step, camera.moved(PanDirection.LEFT).center.longitude, DELTA)
        assertEquals(60.0 + step / 2, camera.moved(PanDirection.UP).center.latitude, DELTA)
        assertEquals(60.0 - step / 2, camera.moved(PanDirection.DOWN).center.latitude, DELTA)
        assertEquals(60.0, camera.moved(PanDirection.RIGHT).center.latitude)
    }

    @Test
    fun movesLessWhenCloser() {
        val far = camera.moved(PanDirection.RIGHT).center.longitude - 17.0
        val closer = camera.zoomedIn()
        val near = closer.moved(PanDirection.RIGHT).center.longitude - 17.0
        assertEquals(far / 2, near, DELTA)
    }

    private companion object {
        const val DELTA = 1e-12
    }
}
