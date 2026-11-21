/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.MapInset
import io.github.dostepnykampus.feature.map.camera.keptUnder
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Ten sam obraz mapy po zmianie dopełnienia kamery przy zamknięciu arkusza. Wymagania: US18.1, NFR-56 */
class SheetClosingTest {
    /** Wiersz: karta sali z wyszukiwania znika, a pasek wraca: środek idzie na południe o połowę obu. */
    @Test
    fun keepsPictureWhenCardAndBarSwap() {
        val shown = VIEW.keptUnder(from = MapInset(bottom = 1200.0), to = MapInset(top = 400.0), pxPerDp = 2f)

        assertEquals(VIEW.shiftedNorth(-400.0).center.latitude, shown.center.latitude, 1e-9, "Obraz mapy skoczył")
    }

    /** Wiersz: karta dotkniętej sali nie zmieniała dopełnienia, więc jej zamknięcie zostawia widok. */
    @Test
    fun keepsViewForSameInset() {
        val inset = MapInset(top = 400.0)

        assertEquals(VIEW, VIEW.keptUnder(from = inset, to = inset, pxPerDp = 2f), "Widok się zmienił")
    }

    private companion object {
        val VIEW = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.11), zoom = 19.0)
    }
}
