/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Przyciski powiększania i przesuwania mapy bez gestów (NFR-53) na punkcie testowym, nie na danych PWr. */
class MapCameraTest {
    private val camera = MapCamera(center = GeoPoint(longitude = 17.0, latitude = 60.0), zoom = START_ZOOM)

    @Test
    fun nfr53_zoom_one_level() {
        assertEquals(START_ZOOM + 1, camera.zoomedIn().zoom)
        assertEquals(START_ZOOM - 1, camera.zoomedOut().zoom)
        assertEquals(camera.center, camera.zoomedIn().center)
    }

    @Test
    fun nfr53_zoom_stays_in_range() {
        val closest = generateSequence(camera) { it.zoomedIn() }.elementAt(index = 10)
        val farthest = generateSequence(camera) { it.zoomedOut() }.elementAt(index = 10)
        assertEquals(20.0, closest.zoom)
        assertEquals(12.0, farthest.zoom)
    }

    @Test
    fun us1_1_building_above_sheet() {
        val building = GeoPoint(longitude = 17.1, latitude = 60.1)
        val aboveSheet = camera.aboveSheet(building, bottomInset = 800.0)
        assertEquals(MapCamera(center = building, zoom = START_ZOOM, bottomInset = 800.0), aboveSheet)
        assertEquals(camera, camera.aboveSheet(focus = null, bottomInset = 800.0))
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
    }
}
