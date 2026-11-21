/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import androidx.compose.ui.unit.dp
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.feature.map.camera.MapCamera
import io.github.dostepnykampus.feature.map.camera.shiftedNorth
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Dotknięta sala zasłonięta kartą przesuwa się w górę tylko w pionie. Wymagania: US18.1, US2.8, NFR-56 */
class RoomRevealTest {
    /** Wiersz: dolny brzeg sali pod kartą staje 16 dp nad kartą, powiększenie zostaje. */
    @Test
    fun liftsRoomCoveredByCard() {
        val shown = VIEW.revealing(area = room(topDp = 150.0, bottomDp = 250.0), free = FREE, card = CARD)

        assertEquals(VIEW.shiftedNorth(-66.0).center.latitude, shown.center.latitude, 1e-9, "Sala nie stoi nad kartą")
        assertEquals(VIEW.zoom, shown.zoom, "Powiększenie się zmieniło")
    }

    /** Wiersz: sala cała nad kartą i sala ucięta dolnym brzegiem ekranu nie ruszają mapy. */
    @Test
    fun keepsVisibleOrCutRoom() {
        val above = VIEW.revealing(area = room(topDp = 0.0, bottomDp = 100.0), free = FREE, card = CARD)
        val cut = VIEW.revealing(area = room(topDp = 200.0, bottomDp = 600.0), free = FREE, card = CARD)

        assertEquals(listOf(VIEW, VIEW), listOf(above, cut), "Mapa ruszyła bez potrzeby")
    }

    /** Wiersz: sala nad kartą to skok kamery bez ruchu na ekranie, sala pod kartą przesuwa mapę w górę. */
    @Test
    fun jumpsOnlyForSeenRoom() {
        val seen = SeenPlan(camera = VIEW, plan = null)
        val map = (FREE + CARD).dp
        val above = seen.tapped(area = null, card = CARD.dp, bar = 0.dp, map = map)
        val under = seen.tapped(area = room(topDp = 150.0, bottomDp = 250.0), card = CARD.dp, bar = 0.dp, map = map)

        assertEquals(listOf(true, false), listOf(above.isJump, under.isJump), "Ruch kamery po dotknięciu sali")
    }

    private companion object {
        const val FREE = 400.0
        const val CARD = 300.0
        val VIEW = MapCamera(center = GeoPoint(longitude = 17.06, latitude = 51.11), zoom = 19.0)

        /** Sala od [topDp] do [bottomDp] poniżej środka wolnej części mapy. */
        fun room(topDp: Double, bottomDp: Double): GeoPolygon {
            val north = VIEW.shiftedNorth(-topDp).center.latitude
            val south = VIEW.shiftedNorth(-bottomDp).center.latitude
            val ring =
                listOf(
                    GeoPoint(longitude = 17.059, latitude = north),
                    GeoPoint(longitude = 17.061, latitude = north),
                    GeoPoint(longitude = 17.061, latitude = south),
                    GeoPoint(longitude = 17.059, latitude = south),
                    GeoPoint(longitude = 17.059, latitude = north),
                )
            return GeoPolygon(listOf(ring))
        }
    }
}
