/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.RoomList
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.FOUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.GROUND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.SECOND
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.VIA_LIFT
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.lift
import io.github.dostepnykampus.feature.map.room.RoomCardFixtures.point
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

/** Karta sali: droga z planów pięter i wejścia zalecanego, kroki pokazu i zamknięcie. Wymagania: US18.1, US18.2 */
@OptIn(ExperimentalCoroutinesApi::class)
class RoomCardViewModelTest {
    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    /** Wiersz: karta otwiera się bez kroku z drogą od wejścia zalecanego przez windę do sali. */
    @Test
    fun loadsWayFromPlansAndEntrance() {
        val card = RoomCardViewModel(Plans(), ENTRANCES)

        card.show(FOUND)

        assertEquals(RoomCard(found = FOUND, way = VIA_LIFT), card.shown.value, "Karta nie wczytała drogi")
    }

    /** Wiersz: „Pokaż po kolei”, „›” i „‹” przestawiają krok, a „Zamknij” zamyka kartę. */
    @Test
    fun walksStepsAndCloses() {
        val card = RoomCardViewModel(Plans(), ENTRANCES)
        card.show(FOUND)

        val steps =
            buildList {
                card.start()
                add(card.shown.value?.step)
                card.next()
                card.next()
                add(card.shown.value?.step)
                card.previous()
                add(card.shown.value?.step)
                card.choose(0)
                add(card.shown.value?.step)
            }
        card.close()

        assertEquals(listOf(0, 2, 1, 0), steps, "Kroki pokazu przeszły źle")
        assertEquals(null, card.shown.value, "„Zamknij” nie zamknął karty")
    }

    /** Nowa sala przerywa wczytywanie drogi poprzedniej, więc karta nie dostaje cudzej drogi. */
    @Test
    fun dropsWayOfPreviousRoom() {
        val slow = CompletableDeferred<Unit>()
        val card = RoomCardViewModel(Plans(waitFor = slow), ENTRANCES)

        card.show(FOUND)
        val other = FOUND.copy(room = FOUND.room.copy(id = "301", number = "301"))
        card.show(other)
        slow.complete(Unit)

        assertEquals(other, card.shown.value?.found, "Karta pokazała poprzednią salę")
    }

    private class Plans(private val waitFor: CompletableDeferred<Unit>? = null) : FloorPlans {
        override suspend fun download(symbol: String): Boolean = false

        override suspend fun floors(symbol: String): FloorList {
            waitFor?.await()
            return FloorList(source = "PWr", floors = listOf(GROUND, SECOND))
        }

        override suspend fun rooms(symbol: String, floor: String): RoomList =
            RoomList(source = "PWr", floorOutline = NO_AREA, rooms = listOf(lift(floor)))
    }

    private companion object {
        val NO_AREA = GeoPolygon(emptyList())

        val ENTRANCES =
            ServerEntrances {
                listOf(
                    Entrance(
                        id = "node/1",
                        buildingSymbol = "A-1",
                        name = "Wejście dostosowane",
                        isMain = false,
                        type = null,
                        location = point(meters = 0.0),
                        marking = EntranceMarking.ACCESSIBLE,
                        floorId = "a1-0",
                    ),
                )
            }
    }
}
