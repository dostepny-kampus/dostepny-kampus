/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.plan

import io.github.dostepnykampus.core.model.building.Floor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Piętra planu w kolumnie pięter: kolejność, napis w kółku i etykieta dla TalkBack. Wymagania: US16.1, NFR-18 */
class FloorColumnTest {
    @Test
    fun listsHighestFloorFirst() {
        val floors =
            listOf(
                Floor(id = "P-1", label = "Piwnica", order = -1),
                Floor(id = "P0", label = "Parter", order = 0),
                Floor(id = "P2", label = "2 piętro", order = 2),
            )
        val plan = ShownPlan(building = "A-1", floors = floors, floor = "P0", rooms = emptyMap())

        val shown = floorOptions(plan).items.map { listOf(it.key, it.short, it.label) }

        val expected =
            listOf(listOf("P2", "2", "2 piętro"), listOf("P0", "0", "Parter"), listOf("P-1", "-1", "Piwnica"))
        assertEquals(expected, shown, "Kolumna nie ma najwyższego piętra u góry z numerem w kółku")
    }
}
