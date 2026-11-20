/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.ENTRANCE
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.GROUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.SECOND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.lift
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Piętro i punkt dla kamery po otwarciu karty lub wyborze kroku. Wymagania: US18.1, US18.2 */
class RoomMapTargetTest {
    @Test
    fun showsRoomBeforeChoosingStep() {
        val card = RoomCard(found = FOUND, way = null)
        val expected = RoomMapTarget(floor = SECOND.id, point = FOUND.room.labelPoint)
        assertEquals(expected, card.mapTarget(), "Sala na planie")
    }

    @Test
    fun followsEntranceLiftAndRoom() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT)
        val targets = (0 until card.stepCount).map { card.chosen(it).mapTarget() }
        val expected =
            listOf(
                RoomMapTarget(floor = GROUND.id, point = ENTRANCE.location),
                RoomMapTarget(floor = GROUND.id, point = lift(GROUND.id).labelPoint),
                RoomMapTarget(floor = SECOND.id, point = FOUND.room.labelPoint),
            )
        assertEquals(expected, targets, "Punkty i piętra kroków drogi")
    }
}
