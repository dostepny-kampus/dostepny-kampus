/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map

import io.github.dostepnykampus.feature.map.room.RoomCard
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Wysokość karty sali dla kamery tylko z pomiaru karty z drogą. Wymagania: US18.2, NFR-56 */
class RoomWayHeightTest {
    /** Wiersz: karta „Wczytujemy drogę…” nie daje wysokości dla kamery, karta z drogą daje swoją. */
    @Test
    fun keepsOnlyHeightWithWay() {
        val signs = SignSheetState(initial = null)
        signs.measureRoom(height = 624, card = RoomCard(found = FOUND, way = null))
        val loading = signs.roomWay
        signs.measureRoom(height = 1229, card = RoomCard(found = FOUND, way = VIA_LIFT))

        assertEquals(listOf(null, FOUND.room.id to 1229), listOf(loading, signs.roomWay), "Wysokość karty dla kamery")
    }
}
