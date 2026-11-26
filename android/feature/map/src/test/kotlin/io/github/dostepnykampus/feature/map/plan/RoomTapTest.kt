/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.campus.ShownPlan
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.SECOND
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Sala z numerem na widocznym piętrze planu daje wynik dotknięcia, korytarz nie. Wymagania: US18.1 */
class RoomTapTest {
    /**
     * Wiersz: dotknięcie sali 329 daje salę budynku i piętra planu, a dotknięcie korytarza nic.
     * Scenariusz: szukanie-sali.feature: Dotknięcie sali z numerem na planie otwiera kartę sali
     */
    @Test
    fun opensNumberedRoomOnly() {
        val corridor =
            FOUND.room.copy(
                id = "k",
                number = null,
                function = "Korytarz",
                category = RoomCategory.CORRIDOR,
                isLabelled = false,
            )
        val list = RoomList(source = "PWr", floorOutline = FOUND.room.geometry, rooms = listOf(FOUND.room, corridor))
        val rooms = mapOf(SECOND.id to list)
        val plan = ShownPlan(building = "A-1", floors = listOf(SECOND), floor = SECOND.id, rooms = rooms)

        assertEquals(listOf(FOUND, null), listOf(plan.roomAt("329"), plan.roomAt("k")), "Dotknięcie sali i korytarza")
    }
}
