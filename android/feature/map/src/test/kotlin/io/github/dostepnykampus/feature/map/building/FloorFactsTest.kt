/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.building

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

/**
 * Fakty o piętrach karty budynku z planów PWr: winda, toaleta dostosowana i opisy pięter.
 *
 * Wymagania: US16.3, US2.3
 */
class FloorFactsTest {
    @Test
    fun findsElevatorOnEveryFloor() {
        val facts = floorFacts(FLOORS, mapOf("P1" to plan(LIFT), "P0" to plan(LIFT), "P-1" to plan(LIFT)))
        assertEquals(
            FloorPresence(floors = listOf(one("Piwnica"), one("Parter"), one("1 piętro")), missing = emptyList()),
            facts.elevator,
            "Winda na każdym piętrze nie dała wszystkich pięter od najniższego bez braków",
        )
    }

    @Test
    fun countsToiletsOnSomeFloors() {
        val facts = floorFacts(FLOORS, mapOf("P-1" to plan(TOILET), "P1" to plan(TOILET, TOILET)))
        assertEquals(
            FloorPresence(floors = listOf(one("Piwnica"), Counted("1 piętro", 2)), missing = listOf("Parter")),
            facts.toilet,
            "Toaleta dostosowana na części pięter nie dała liczby i pięter bez niej",
        )
    }

    @Test
    fun dropsElevatorMissingFromPlans() {
        val facts = floorFacts(FLOORS, mapOf("P0" to plan(TOILET)))
        assertEquals(null, facts.elevator, "Budynek bez windy w planach dostał wiersz windy")
    }

    @Test
    fun describesByCommonFunction() {
        val facts = floorFacts(FLOORS, mapOf("P0" to plan(room("Portiernia"), room("Sala"), room("Sala"), CORRIDOR)))
        assertEquals(
            listOf(Counted("Sala", 2), one("Portiernia")),
            facts.floors.single { it.floor.id == "P0" }.functions,
            "Opis piętra nie zaczyna od najczęstszej funkcji albo zawiera korytarz",
        )
    }

    @Test
    fun skipsFloorWithoutFunctions() {
        val facts = floorFacts(FLOORS, mapOf("P1" to plan(LIFT, TOILET)))
        assertEquals(
            listOf(emptyList<Counted>(), emptyList(), emptyList()),
            facts.floors.map { it.functions },
            "Piętro z samą windą i toaletą albo bez planu dostało opis",
        )
    }

    private fun one(name: String) = Counted(name = name, count = 1)

    private fun plan(vararg rooms: Room) = RoomList(source = SOURCE, floorOutline = NO_OUTLINE, rooms = rooms.toList())

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        val NO_OUTLINE = GeoMultiPolygon(emptyList())
        val FLOORS =
            listOf(
                Floor(id = "P1", label = "1 piętro", order = 1),
                Floor(id = "P-1", label = "Piwnica", order = -1),
                Floor(id = "P0", label = "Parter", order = 0),
            )
        val LIFT = room("Winda", RoomCategory.ELEVATOR)
        val TOILET = room("Toaleta dla niepełnosprawnych", RoomCategory.ACCESSIBLE_TOILET)
        val CORRIDOR = room("Korytarz", RoomCategory.CORRIDOR)

        fun room(function: String, category: RoomCategory = RoomCategory.OTHER) =
            Room(
                id = function,
                number = null,
                function = function,
                category = category,
                geometry = NO_OUTLINE,
                labelPoint = GeoPoint(longitude = 17.0, latitude = 51.0),
                isLabelled = false,
            )
    }
}
