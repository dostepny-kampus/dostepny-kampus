/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.camera

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Ruch mapy bez arkusza według odległości celu, jak ruch do budynku. Wymagania: US2.8, US18.1, NFR-56 */
class UncoveredMoveTest {
    /** Wiersz: cel w zasięgu ekranu, np. dotknięta sala, to przesunięcie bez przelotu. */
    @Test
    fun slidesToNearView() {
        val move = view(latitude = 51.1072).uncoveredMove(closed = null, from = REST, free = FREE)

        assertEquals(CameraMotion.EASE, move.motion, "Bliski cel poszedł przelotem")
    }

    /** Wiersz: cel dalej niż ekran i ruch bez znanego widoku to przelot przycisków. */
    @Test
    fun fliesToFarView() {
        val far = view(latitude = 51.11).uncoveredMove(closed = null, from = REST, free = FREE)
        val unknown = view(latitude = 51.1072).uncoveredMove(closed = null, from = null, free = FREE)

        assertEquals(
            listOf(CameraMotion.FLIGHT, CameraMotion.FLIGHT),
            listOf(far.motion, unknown.motion),
            "Daleki albo nieznany start bez przelotu",
        )
    }

    /** Wiersz: po ruchu aplikacji (bez gestu) ruch do sali zna widok startowy, także gdy cel jest już zapisany. */
    @Test
    fun knowsOriginAfterAppMove() {
        val last = ShownCamera()
        val target = view(latitude = 51.1072)
        last.keep(shown = REST, inset = MapInset())
        val before = last.originOf(target)
        last.keep(shown = target, inset = MapInset())

        assertEquals(listOf(REST, REST), listOf(before, last.originOf(target)), "Ruch bez widoku startowego")
    }

    private companion object {
        val FREE = FreeMap(widthDp = 411.0, heightDp = 400.0, endDp = 0.0, marginDp = 24.0)
        val REST = view(latitude = 51.107)

        fun view(latitude: Double) = MapCamera(center = GeoPoint(longitude = 17.059, latitude = latitude), zoom = 19.0)
    }
}
