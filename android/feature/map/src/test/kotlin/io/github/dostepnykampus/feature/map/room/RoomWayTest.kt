/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.room

import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.room.FoundRoom
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import org.junit.Assert.assertEquals
import org.junit.Test

private const val METERS_PER_LON_AT_CAMPUS = 69_970.0
private const val LAT = 51.11
private const val LON = 17.06

/** Kroki do sali: wejście zalecane, winda i sala albo powód braku drogi. Wymagania: US18.2, US18.3, BR-25 */
class RoomWayTest {
    private val ground = Floor(id = "p0", label = "Parter", order = 0)
    private val second = Floor(id = "p2", label = "2 piętro", order = 2)
    private val floors = listOf(ground, Floor(id = "p1", label = "1 piętro", order = 1), second)

    /**
     * Wiersz: winda staje na piętrze wejścia i na piętrze sali; odległości w linii prostej, zaokrąglone do 5 m.
     * Scenariusz (częściowo): szukanie-sali.feature:
     *   Kroki do sali podane tekstem bez rysowania trasy
     */
    @Test
    fun leadsThroughLift() {
        val way = way(entrance(floor = "p0"), *liftThrough())

        val lift = RoomWay.Lift(id = "w", metersFromEntrance = 30)
        val steps =
            RoomWay.Steps(
                entrance = entrance(floor = "p0"),
                entranceFloor = ground,
                isFloorAssumed = false,
                lift = lift,
                roomMeters = 85,
            )
        assertEquals("winda", steps, way)
    }

    /** Wiersz: winda, która nie staje na piętrze sali, nie liczy się, choć jest bliżej wejścia. */
    @Test
    fun picksLiftToRoomFloor() {
        val near = lift(id = "blisko", at = 5.0, floor = "p0").copy(liftFloors = listOf("p0", "p1"))
        val far = lift(id = "daleko", at = 40.0, floor = "p0")

        val way = way(entrance(floor = "p0"), near, far, lift(id = "daleko", at = 40.0, floor = "p2"))

        assertEquals("winda na piętro sali", "daleko", (way as RoomWay.Steps).lift?.id)
    }

    /**
     * Wiersz: sala na piętrze wejścia ma kroki bez windy, z odległością od wejścia.
     * Scenariusz (częściowo): szukanie-sali.feature:
     *   Kroki do sali podane tekstem bez rysowania trasy
     */
    @Test
    fun skipsLiftOnSameFloor() {
        val way = way(entrance(floor = "p2"))

        val steps =
            RoomWay.Steps(
                entrance = entrance(floor = "p2"),
                entranceFloor = second,
                isFloorAssumed = false,
                lift = null,
                roomMeters = 115,
            )
        assertEquals("bez windy", steps, way)
    }

    /**
     * Wiersz: bez piętra wejścia w danych krok przyjmuje parter i mówi to.
     * Scenariusz (częściowo): szukanie-sali.feature:
     *   Kroki do sali podane tekstem bez rysowania trasy
     */
    @Test
    fun assumesGroundFloor() {
        val way = way(entrance(floor = null), *liftThrough())

        assertEquals("parter przyjęty", ground to true, (way as RoomWay.Steps).entranceFloor to way.isFloorAssumed)
    }

    /**
     * Wiersz: brak wejścia zalecanego albo windy z piętra wejścia na piętro sali daje powód zamiast kroków.
     * Scenariusz (częściowo): szukanie-sali.feature:
     *   Bez sali w danych albo bez drogi bez schodów aplikacja mówi to zdaniem
     */
    @Test
    fun namesMissingWay() {
        assertEquals("brak wejścia", RoomWay.NoEntrance, way(entrance = null))
        assertEquals("brak windy", RoomWay.NoLift, way(entrance(floor = "p0"), lift(id = "w", at = 5.0, floor = "p1")))
    }

    private fun way(entrance: Entrance?, vararg rooms: Pair<String, Room>): RoomWay =
        roomWay(
            found = FoundRoom(building = "A-1", floor = second, room = ROOM),
            entrance = entrance,
            floors = floors,
            rooms = { floor -> rooms.filter { it.first == floor }.map { it.second } },
        )

    private fun entrance(floor: String?) =
        Entrance(
            id = "node/1",
            buildingSymbol = "A-1",
            name = "Wejście dostosowane",
            isMain = false,
            type = null,
            location = GeoPoint(longitude = LON, latitude = LAT),
            marking = EntranceMarking.ACCESSIBLE,
            floorId = floor,
        )

    private fun liftThrough(): Array<Pair<String, Room>> =
        arrayOf(lift(id = "w", at = 28.0, floor = "p0"), lift(id = "w", at = 28.0, floor = "p2"))

    private fun lift(id: String, at: Double, floor: String): Pair<String, Room> {
        val room =
            Room(
                id = "$id-$floor",
                number = null,
                function = "Winda",
                category = RoomCategory.ELEVATOR,
                geometry = NO_OUTLINE,
                labelPoint = point(at),
                isLabelled = false,
                liftId = id,
                liftFloors = listOf("p0", "p1", "p2"),
            )
        return floor to room
    }

    private fun Pair<String, Room>.copy(liftFloors: List<String>): Pair<String, Room> =
        first to second.copy(liftFloors = liftFloors)

    private companion object {
        val NO_OUTLINE = GeoMultiPolygon(emptyList())

        val ROOM =
            Room(
                id = "329",
                number = "329",
                function = "Sala wykładowa",
                category = RoomCategory.LECTURE_HALL,
                geometry = NO_OUTLINE,
                labelPoint = point(meters = 113.0),
                isLabelled = true,
            )

        fun point(meters: Double) = GeoPoint(longitude = LON + meters / METERS_PER_LON_AT_CAMPUS, latitude = LAT)
    }
}
