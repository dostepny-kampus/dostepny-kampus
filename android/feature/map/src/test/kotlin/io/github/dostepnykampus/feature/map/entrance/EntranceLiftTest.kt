/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.map.entrance

import io.github.dostepnykampus.core.data.room.FloorPlans
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.EntranceMarking
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Metry na stopień długości geograficznej przy szerokości kampusu, 51,11°. */
private const val METERS_PER_LON = 69_970.0
private const val LAT = 51.11
private const val LON = 17.06

/** Najbliższa winda wejścia z planu PWr i zakres jej pięter. Wymagania: US3.3, US16.3, BR-25 */
class EntranceLiftTest {
    private val floors =
        listOf(
            Floor(id = "p-1", label = "Piwnica", order = -1),
            Floor(id = "p0", label = "Parter", order = 0),
            Floor(id = "p3", label = "3 piętro", order = 3),
        )

    /**
     * Wiersz: najbliższa winda na piętrze wejścia, odległość w linii prostej zaokrąglona do 5 m i piętra windy
     *   od najniższego.
     * Scenariusz (częściowo, przykład: 28 m): przegladanie-mapy.feature: Część „Za drzwiami” podaje najbliższą windę
     *   i jej piętra
     */
    @Test
    fun findsNearestLift() =
        runTest {
            val plans = Plans(lift(id = "daleko", meters = 60.0), lift(id = "blisko", meters = 28.0))

            val lift = plans.liftNear(entrance())

            val stops = listOf("Piwnica", "Parter", "3 piętro")
            assertEquals(EntranceLift(entranceId = "node/1", meters = 30, floors = stops), lift)
        }

    /** Wiersz: odległość zaokrąglona do 5 m, najmniej 5 m. */
    @Test
    fun roundsToFiveMeters() =
        runTest {
            assertEquals(10, Plans(lift(meters = 12.0)).liftNear(entrance())?.meters)
            assertEquals(5, Plans(lift(meters = 1.0)).liftNear(entrance())?.meters)
        }

    /** Wiersz: wejście z piętrem w danych szuka windy na tym piętrze, a nie na parterze. */
    @Test
    fun usesEntranceFloor() =
        runTest {
            val plans = Plans(lift(meters = 20.0), floor = "p-1")

            assertEquals(20, plans.liftNear(entrance(floor = "p-1"))?.meters)
            assertNull("parter bez windy", plans.liftNear(entrance()))
        }

    /**
     * Wiersz: bez windy na planie albo bez planu budynku części „Za drzwiami” nie ma.
     * Scenariusz: przegladanie-mapy.feature: Bez windy na planie arkusz wejścia nie ma części „Za drzwiami”
     */
    @Test
    fun skipsWithoutLift() =
        runTest {
            assertNull("bez windy", Plans().liftNear(entrance()))
            assertNull("bez planu", Plans(lift(meters = 20.0), isStored = false).liftNear(entrance()))
        }

    /**
     * Wiersz: zakres pięter z odmianą piwnicy, parteru i „N piętro”; inna nazwa piętra daje listę.
     * Scenariusz: przegladanie-mapy.feature: Część „Za drzwiami” podaje najbliższą windę i jej piętra
     */
    @Test
    fun namesFloorRange() {
        assertEquals("piwnicy" to "3 piętra", liftRange(listOf("Piwnica", "Parter", "3 piętro")))
        assertEquals("parteru" to "1 piętra", liftRange(listOf("Parter", "1 piętro")))
        assertNull("nazwa spoza odmiany", liftRange(listOf("Piwnica", "Parter", "Antresola")))
        assertNull("jedno piętro", liftRange(listOf("Parter")))
    }

    private fun entrance(floor: String? = null) =
        Entrance(
            id = "node/1",
            buildingSymbol = "A-1",
            name = null,
            isMain = false,
            type = null,
            location = GeoPoint(longitude = LON, latitude = LAT),
            marking = EntranceMarking.ACCESSIBLE,
            floorId = floor,
        )

    private fun lift(id: String = "w", meters: Double) =
        Room(
            id = id,
            number = null,
            function = "Winda",
            category = RoomCategory.ELEVATOR,
            geometry = NO_OUTLINE,
            labelPoint = GeoPoint(longitude = LON + meters / METERS_PER_LON, latitude = LAT),
            isLabelled = false,
            liftId = id,
            liftFloors = listOf("p3", "p0", "p-1"),
        )

    /** Plan A-1 z [lifts] na piętrze [floor] i schodami; bez kopii, gdy nie [isStored]. */
    private inner class Plans(
        vararg val lifts: Room,
        private val floor: String = "p0",
        private val isStored: Boolean = true,
    ) : FloorPlans {
        override suspend fun download(symbol: String): Boolean = false

        override suspend fun floors(symbol: String): FloorList? =
            FloorList(source = SOURCE, floors = this@EntranceLiftTest.floors).takeIf { isStored }

        override suspend fun rooms(symbol: String, floor: String): RoomList? {
            val rooms = if (floor == this.floor) lifts.toList() + STAIRS else listOf(STAIRS)
            return RoomList(source = SOURCE, floorOutline = NO_OUTLINE, rooms = rooms).takeIf { isStored }
        }
    }

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        val NO_OUTLINE = GeoMultiPolygon(emptyList())
        val STAIRS =
            Room(
                id = "s",
                number = null,
                function = "Schody",
                category = RoomCategory.STAIRCASE,
                geometry = NO_OUTLINE,
                labelPoint = GeoPoint(longitude = LON, latitude = LAT),
                isLabelled = false,
            )
    }
}
