/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import io.github.dostepnykampus.core.model.CampusBounds
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Komunikat TalkBack o końcu terenu przy brzegu na punkcie testowym, nie na danych PWr. Wymagania: US1.7, NFR-19 */
class AreaEdgeNoticeTest {
    /**
     * Scenariusz „Przy granicy terenu TalkBack mówi, jak zmienić teren”: dojście do brzegu daje jedno ogłoszenie,
     * drugie dopiero po odsunięciu widoku od brzegu i powrocie.
     *
     * Wymagania: US1.7, NFR-19
     */
    @Test
    fun announcesOnceUntilEdgeLeft() {
        val views = listOf(point(), point(longitude = 17.1), point(longitude = 17.1), point(), point(longitude = 17.1))
        val names = views.map { areaEdgeName(area = AREA, atEdge = it.isAtEdgeOf(BOUNDS)) }
        assertEquals(listOf(null, AREA, AREA, null, AREA), names, "nazwa terenu w kolejnych widokach")
    }

    @Test
    fun changesNameAfterAreaChange() {
        assertEquals(AREA, areaEdgeName(area = AREA, atEdge = true), "teren przed zmianą")
        assertEquals("Wittigowo", areaEdgeName(area = "Wittigowo", atEdge = true), "teren po zmianie")
    }

    @Test
    fun staysSilentWithoutChosenArea() {
        assertEquals(null, areaEdgeName(area = null, atEdge = true), "brak nazwy terenu")
    }

    @Test
    fun detectsEveryEdgeOfBounds() {
        val sides = listOf(point(longitude = 16.9), point(longitude = 17.1))
        val ends = listOf(point(latitude = 59.95), point(latitude = 60.05))
        assertEquals(listOf(true, true), sides.map { it.isAtEdgeOf(BOUNDS) }, "brzegi zachodni i wschodni")
        assertEquals(listOf(true, true), ends.map { it.isAtEdgeOf(BOUNDS) }, "brzegi południowy i północny")
        assertEquals(false, point().isAtEdgeOf(BOUNDS), "środek terenu")
    }

    private fun point(longitude: Double = 17.0, latitude: Double = 60.0): MapCamera =
        MapCamera(center = GeoPoint(longitude = longitude, latitude = latitude), zoom = 17.0)

    private companion object {
        const val AREA = "Kampus główny"
        val BOUNDS = CampusBounds(west = 16.9, south = 59.95, east = 17.1, north = 60.05)
    }
}
