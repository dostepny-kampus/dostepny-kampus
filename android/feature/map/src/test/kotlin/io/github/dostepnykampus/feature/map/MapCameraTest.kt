/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.data.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.cos
import kotlin.math.pow

/** Przyciski powiększania i przesuwania mapy bez gestów na punkcie testowym, nie na danych PWr. Wymagania: NFR-53 */
class MapCameraTest {
    private val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)

    @Test
    fun nfr53_zoom_one_level() {
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
    fun nfr53_zoom_stays_in_range() {
        val campus = CampusBounds(west = 16.9, south = 59.95, east = 17.1, north = 60.05)
        val limits = MapLimits(campus, widthDp = 411.0, heightDp = 800.0)
        val closest = generateSequence(camera) { it.zoomedIn() }.elementAt(index = 10)
        val farthest = generateSequence(camera) { limits.clamp(it.zoomedOut()) }.elementAt(index = 10)
        assertEquals(20.0, closest.zoom)
        assertEquals(limits.minZoom, farthest.zoom)
    }

    /**
     * Scenariusz „Przy małej karcie budynek jest cały widoczny, a mapą sterują przyciski”: prostokąt budynku mieści
     * się w wolnej części mapy na lewo od pasa przycisków, z odstępem od brzegów.
     *
     * Wymagania: US2.8, US1.4
     */
    @Test
    fun us2_8_building_fits_free_map() {
        val outline = CampusBounds(west = 17.0, south = 60.0, east = 17.004, north = 60.001)
        val free = FreeMap(widthDp = 411.0, heightDp = 400.0, endDp = 160.0, marginDp = 24.0)
        val fit = fitted(outline, free)
        val dpPerDegree = 512.0 * 2.0.pow(fit.zoom) / 360.0
        val left = free.widthDp / 2 - (fit.center.longitude - outline.west) * dpPerDegree
        val right = free.widthDp / 2 + (outline.east - fit.center.longitude) * dpPerDegree
        assertEquals(24.0, left, DP)
        assertEquals(411.0 - 160.0 - 24.0, right, DP)
        assertEquals(60.0005, fit.center.latitude, DELTA)
        val tall = (outline.north - outline.south) * dpPerDegree / cos(Math.toRadians(60.0005))
        assertEquals(true, tall <= free.heightDp - 2 * free.marginDp)
    }

    /** Wiersz: mały budynek nie jest powiększany ponad poziom 18, więc sąsiednie budynki zostają widoczne. */
    @Test
    fun us2_8_small_keeps_context() {
        val outline = CampusBounds(west = 17.0, south = 60.0, east = 17.0001, north = 60.0001)
        val free = FreeMap(widthDp = 411.0, heightDp = 400.0, endDp = 0.0, marginDp = 24.0)
        val fit = fitted(outline, free)
        assertEquals(18.0, fit.zoom)
        assertEquals(17.00005, fit.center.longitude, DELTA)
    }

    @Test
    fun nfr53_arrows_move_center() {
        val step = 70.0 / 131_072.0
        assertEquals(17.0 + step, camera.moved(PanDirection.RIGHT).center.longitude, DELTA)
        assertEquals(17.0 - step, camera.moved(PanDirection.LEFT).center.longitude, DELTA)
        assertEquals(60.0 + step / 2, camera.moved(PanDirection.UP).center.latitude, DELTA)
        assertEquals(60.0 - step / 2, camera.moved(PanDirection.DOWN).center.latitude, DELTA)
        assertEquals(60.0, camera.moved(PanDirection.RIGHT).center.latitude)
    }

    @Test
    fun nfr53_closer_view_moves_less() {
        val far = camera.moved(PanDirection.RIGHT).center.longitude - 17.0
        val closer = camera.zoomedIn()
        val near = closer.moved(PanDirection.RIGHT).center.longitude - 17.0
        assertEquals(far / 2, near, DELTA)
    }

    private companion object {
        const val DELTA = 1e-12
        const val DP = 1e-6
    }
}
