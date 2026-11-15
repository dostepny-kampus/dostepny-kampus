/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.area

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.campus.CampusBounds
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
    fun announcesOnceUntilViewLeavesEdge() {
        val notice = AreaEdgeNotice()
        val steps = listOf(point(longitude = 17.0), point(longitude = 17.1), point(longitude = 17.1))
        val back = listOf(point(longitude = 17.0), point(longitude = 17.1))
        val announced = (steps + back).map { notice.isDue(area = AREA, atEdge = it.isAtEdgeOf(BOUNDS)) }
        assertEquals(listOf(false, true, false, false, true), announced, "ogłoszenia przy kolejnych widokach")
    }

    @Test
    fun announcesAgainAfterAreaChange() {
        val notice = AreaEdgeNotice()
        assertEquals(true, notice.isDue(area = AREA, atEdge = true), "pierwsze ogłoszenie")
        assertEquals(true, notice.isDue(area = "Wittigowo", atEdge = true), "ogłoszenie po zmianie terenu")
        assertEquals(false, notice.isDue(area = "Wittigowo", atEdge = true), "powtórka bez odsunięcia")
    }

    @Test
    fun staysSilentWithoutChosenArea() {
        assertEquals(false, AreaEdgeNotice().isDue(area = null, atEdge = true), "brak nazwy terenu")
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
