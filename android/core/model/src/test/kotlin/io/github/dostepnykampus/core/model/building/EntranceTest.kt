/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.building

import io.github.dostepnykampus.core.model.GeoPoint
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/**
 * Wejście jako wartość: zmiana oznaczenia daje nowy stan tego samego wejścia.
 *
 * Wymagania: US17.1, US17.2, BR-24, US2.1, BR-25
 */
class EntranceTest {
    @Test
    fun keepsEntranceForSameData() {
        val reloaded = sideEntrance().copy()
        assertEquals(sideEntrance(), reloaded, "wejście z tych samych danych jest równe")
        assertEquals(sideEntrance().hashCode(), reloaded.hashCode(), "skrót wejścia z tych samych danych jest równy")
    }

    @Test
    fun keepsEntranceOnChange() {
        val changed = sideEntrance().copy(marking = EntranceMarking.INACCESSIBLE)
        assertEquals(EntranceMarking.INACCESSIBLE, changed.marking, "zmiana oznaczenia daje nowe oznaczenie")
        assertEquals(
            sideEntrance(),
            changed.copy(marking = sideEntrance().marking),
            "przywrócone oznaczenie daje wejście równe pierwotnemu",
        )
    }

    @Test
    fun makesNewStateOnChange() {
        assertNotEquals(
            sideEntrance(),
            sideEntrance().copy(marking = EntranceMarking.ACCESSIBLE),
            "zmiana oznaczenia daje wejście różne od pierwotnego",
        )
    }

    @Test
    fun distinguishesByMainFlag() {
        assertNotEquals(
            sideEntrance(),
            sideEntrance().copy(isMain = true),
            "zmiana flagi głównego wejścia daje wejście różne",
        )
    }

    @Test
    fun filtersEntranceTypes() {
        val types = listOf("main", "emergency", "service", "exit", "staircase", "garage", "yes", null)
        val visitors = types.map { sideEntrance().copy(type = it) }.filter(Entrance::isForVisitors)

        assertEquals(
            listOf("main", "service", "staircase", "yes", null),
            visitors.map(Entrance::type),
            "typy wejść odwiedzających po przefiltrowaniu",
        )
    }

    private fun sideEntrance(): Entrance =
        Entrance(
            id = "B-4-boczne",
            buildingSymbol = "B-4",
            name = "Wejście boczne",
            isMain = false,
            type = "yes",
            location = GeoPoint(longitude = 17.0625, latitude = 51.109375),
            marking = EntranceMarking.NO_DATA,
        )
}
