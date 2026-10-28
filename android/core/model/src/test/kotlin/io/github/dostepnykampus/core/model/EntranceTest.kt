/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Test

/** Wejście jako wartość: zmiana oznaczenia daje nowy stan tego samego wejścia (US17.1, US17.2, BR-24). */
class EntranceTest {
    private val sideEntrance =
        Entrance(
            id = "B-4-boczne",
            buildingSymbol = "B-4",
            name = "Wejście boczne",
            isMain = false,
            location = GeoPoint(longitude = 17.0625, latitude = 51.109375),
            marking = EntranceMarking.NO_DATA,
        )

    @Test
    fun us17_1_same_data_same_entrance() {
        val reloaded = sideEntrance.copy()
        assertEquals(sideEntrance, reloaded)
        assertEquals(sideEntrance.hashCode(), reloaded.hashCode())
    }

    @Test
    fun us17_2_change_keeps_entrance() {
        val changed = sideEntrance.copy(marking = EntranceMarking.INACCESSIBLE)
        assertEquals(EntranceMarking.INACCESSIBLE, changed.marking)
        assertEquals(sideEntrance, changed.copy(marking = sideEntrance.marking))
    }

    @Test
    fun us17_2_change_is_new_state() {
        assertNotEquals(sideEntrance, sideEntrance.copy(marking = EntranceMarking.ACCESSIBLE))
    }

    @Test
    fun us2_1_main_flag_distinguishes() {
        assertNotEquals(sideEntrance, sideEntrance.copy(isMain = true))
    }
}
