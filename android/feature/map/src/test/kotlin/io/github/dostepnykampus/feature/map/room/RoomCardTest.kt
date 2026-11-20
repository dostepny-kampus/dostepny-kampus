/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/** Kroki karty sali: bez wybranego kroku na start, wybór, poprzedni i następny. Wymagania: US18.2 */
class RoomCardTest {
    /** Wiersz: karta otwiera się bez wybranego kroku, a „Pokaż po kolei” wybiera krok 1. */
    @Test
    fun startsWithoutStep() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT)

        assertEquals(listOf(null, 0), listOf(card.step, card.started().step), "Karta nie zaczęła bez kroku")
    }

    /** Wiersz: droga przez windę ma trzy kroki, a sala na piętrze wejścia dwa. */
    @Test
    fun countsSteps() {
        val sameFloor = VIA_LIFT.copy(lift = null)

        assertEquals(
            listOf(3, 2, 0),
            listOf(VIA_LIFT, sameFloor, RoomWay.NoLift).map { RoomCard(found = FOUND, way = it).stepCount },
            "Zła liczba kroków",
        )
    }

    /** Wiersz: „‹” wyłączona na pierwszym kroku, „›” na ostatnim. */
    @Test
    fun limitsArrowsAtEnds() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT)

        val ends = listOf(card.chosen(0), card.chosen(1), card.chosen(2)).map { it.hasPrevious to it.hasNext }

        assertEquals(listOf(false to true, true to true, true to false), ends, "Strzałki na złych krokach")
    }

    /** Wiersz: strzałki przechodzą o jeden krok i nie wychodzą poza kroki drogi. */
    @Test
    fun movesByOneStep() {
        val card = RoomCard(found = FOUND, way = VIA_LIFT).chosen(1)

        val moved = listOf(card.next(), card.previous(), card.next().next(), card.previous().previous())

        assertEquals(listOf(2, 0, 2, 0), moved.map { it.step }, "Strzałki przeszły źle")
    }

    /** Wiersz: bez drogi i w czasie wczytywania kroku nie da się wybrać. */
    @Test
    fun ignoresStepWithoutWay() {
        val cards = listOf(null, RoomWay.NoEntrance).map { RoomCard(found = FOUND, way = it) }

        assertEquals(listOf(null, null), cards.map { it.started().chosen(1).step }, "Wybrano krok bez drogi")
    }
}
