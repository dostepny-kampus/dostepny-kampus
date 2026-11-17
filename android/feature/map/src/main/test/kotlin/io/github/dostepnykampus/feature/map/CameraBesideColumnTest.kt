/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.START_ZOOM
import io.github.dostepnykampus.feature.map.camera.besideStart
import io.github.dostepnykampus.feature.map.campus.CampusBounds
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.math.pow

private const val WIDTH_DP = 411.0
private const val COLUMN_DP = 80.0
private const val DP = 0.5

/**
 * Widok po „Pokaż na planie” obok kolumny pięter przy lewym brzegu mapy, na punkcie testowym.
 *
 * Wymagania: US16.1, US1.3, US1.4, NFR-17
 */
class CameraBesideColumnTest {
    private val view = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.11), zoom = START_ZOOM)

    /** Budynek węższy niż mapa obok kolumny stoi na środku tej części, nie pod kolumną. */
    @Test
    fun centersNarrowBuildingBesideColumn() {
        val narrow = outline(widthDp = 200.0)
        val moved = view.besideStart(outline = narrow, widthDp = WIDTH_DP, startDp = COLUMN_DP)
        val west = screenDp(longitude = narrow.west, camera = moved)
        val east = screenDp(longitude = narrow.east, camera = moved)
        assertEquals(west - COLUMN_DP, WIDTH_DP - east, DP, "budynek na środku części obok kolumny")
    }

    /** Budynek szerszy niż mapa obok kolumny zaczyna się tuż za kolumną i wystaje w prawo. */
    @Test
    fun startsWideBuildingAfterColumn() {
        val wide = outline(widthDp = 380.0)
        val moved = view.besideStart(outline = wide, widthDp = WIDTH_DP, startDp = COLUMN_DP)
        assertEquals(COLUMN_DP, screenDp(longitude = wide.west, camera = moved), DP, "lewy brzeg budynku za kolumną")
    }

    /** Przesunięcie nie zmienia powiększenia ani szerokości geograficznej środka. */
    @Test
    fun keepsZoomAndLatitude() {
        val moved = view.besideStart(outline = outline(widthDp = 200.0), widthDp = WIDTH_DP, startDp = COLUMN_DP)
        assertEquals(view.zoom, moved.zoom, "powiększenie zostaje")
        assertEquals(view.center.latitude, moved.center.latitude, "szerokość geograficzna zostaje")
    }

    private fun outline(widthDp: Double): CampusBounds {
        val half = widthDp / 2 / dpPerDegree()
        return CampusBounds(west = 17.06 - half, south = 51.1099, east = 17.06 + half, north = 51.1101)
    }

    private fun screenDp(longitude: Double, camera: MapCamera): Double =
        WIDTH_DP / 2 + (longitude - camera.center.longitude) * dpPerDegree()

    private fun dpPerDegree(): Double = 512.0 * 2.0.pow(START_ZOOM) / 360.0
}
