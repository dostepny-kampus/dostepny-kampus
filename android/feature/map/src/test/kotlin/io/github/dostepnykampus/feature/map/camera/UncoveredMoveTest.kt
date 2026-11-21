/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Ruch mapy bez arkusza: przesunięcie bez zmiany powiększenia, przelot ze zmianą. Wymagania: US18.1, NFR-56 */
class UncoveredMoveTest {
    /** Wiersz: sala dotknięta przy tym samym powiększeniu przesuwa mapę płynnie, bez przelotu. */
    @Test
    fun slidesAtSameZoom() {
        val move = TARGET.uncoveredMove(closed = null, from = REST)

        assertEquals(CameraMotion.EASE, move.motion, "Ruch bez zmiany powiększenia poszedł przelotem")
    }

    /** Wiersz: ruch ze zmianą powiększenia i ruch bez znanego widoku to przelot przycisków. */
    @Test
    fun fliesOnZoomChange() {
        val zoomed = TARGET.uncoveredMove(closed = null, from = REST.copy(zoom = 17.0))
        val unknown = TARGET.uncoveredMove(closed = null)

        assertEquals(
            listOf(CameraMotion.FLIGHT, CameraMotion.FLIGHT),
            listOf(zoomed.motion, unknown.motion),
            "Ruch ze zmianą powiększenia bez przelotu",
        )
    }

    private companion object {
        val REST = MapCamera(center = GeoPoint(longitude = 17.059, latitude = 51.107), zoom = 19.0)
        val TARGET = MapCamera(center = GeoPoint(longitude = 17.059, latitude = 51.108), zoom = 19.0)
    }
}
