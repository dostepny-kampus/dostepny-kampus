/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.room

import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.database.room.FloorPlanEntity
import io.github.dostepnykampus.core.database.room.FloorRoomEntity
import io.github.dostepnykampus.core.database.room.FloorRoomFact
import io.github.dostepnykampus.core.model.GeoMultiPolygon
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.GeoPolygon
import io.github.dostepnykampus.core.model.building.Floor
import io.github.dostepnykampus.core.model.building.FloorList
import io.github.dostepnykampus.core.model.room.Room
import io.github.dostepnykampus.core.model.room.RoomCategory
import io.github.dostepnykampus.core.model.room.RoomList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.coroutines.EmptyCoroutineContext

/**
 * Kopia planu budynku: pobranie całości z piętrami windy, odczyt bez sieci, stara kopia przy błędzie.
 *
 * Wymagania: US16.1, NFR-10
 */
class FloorPlanCopyTest {
    @Test
    fun readsCopyWithoutNetwork() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            val online = dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) })
            val offline = dao.copy(floors = { ServerReply.Offline }, rooms = { _, _ -> ServerReply.Offline })
            assertEquals(true, online.download(C13), "Pobranie planu z serwera się nie udało")
            assertEquals(false, offline.download(C13), "Pobranie bez sieci zgłosiło sukces")
            val read = listOf("P0", "P1", "P9").map { offline.rooms(symbol = C13, floor = it) }
            val expected = listOf(FLOORS, plan("P0"), plan("P1"), null)
            assertEquals(expected, listOf(offline.floors(C13)) + read, "Odczyt kopii bez sieci nie zgadza się")
        }

    @Test
    fun keepsCopyWhenFloorFails() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) }).download(C13)
            val broken =
                dao.copy(rooms = { _, floor -> if (floor == "P1") ServerReply.Offline else ServerReply.Ok(EMPTY) })
            assertEquals(false, broken.download(C13), "Błąd piętra zgłosił sukces")
            assertEquals(plan("P0"), broken.rooms(symbol = C13, floor = "P0"), "Błąd piętra zmienił kopię")
        }

    @Test
    fun clearsCopyOfBasicBuilding() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) }).download(C13)
            val basic = dao.copy(floors = { ServerReply.Ok(FloorList(SOURCE, emptyList())) })
            assertEquals(true, basic.download(C13), "Budynek bez pięter nie dał sukcesu")
            val left = listOf(basic.floors(C13), basic.rooms(symbol = C13, floor = "P0"))
            assertEquals(listOf(null, null), left, "Kopia budynku bez pięter została")
        }

    @Test
    fun givesNothingForBrokenCopy() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) }).download(C13)
            dao.rooms.replaceAll { it.copy(category = "GARAGE") }
            assertEquals(null, dao.copy().rooms(symbol = C13, floor = "P0"), "Zepsuta kopia dała plan")
        }

    /** Wiersz: piętro odczytane raz idzie z pamięci, a nowe pobranie planu budynku daje nowe pomieszczenia. */
    @Test
    fun keepsReadFloorInMemory() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            var floor = plan("P0")
            val copy = dao.copy(rooms = { _, _ -> ServerReply.Ok(floor) })
            copy.download(C13)
            val first = copy.rooms(symbol = C13, floor = "P0")
            dao.rooms.clear()
            val kept = copy.rooms(symbol = C13, floor = "P0")
            floor = plan("P1")
            copy.download(C13)
            val fresh = copy.rooms(symbol = C13, floor = "P0")

            assertEquals(listOf(first, plan("P1")), listOf(kept, fresh), "Pamięć piętra")
        }

    @Test
    fun readsRoomFactsWithoutGeometry() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) }).download(C13)
            dao.plans.replaceAll { it.copy(outlineJson = "{zepsute") }
            dao.rooms.replaceAll { it.copy(geometryJson = "{zepsute") }
            val expected = mapOf("P0" to floorSummaries(), "P1" to floorSummaries())
            assertEquals(expected, dao.copy().roomFacts(C13), "Fakty o piętrach nie powinny dekodować geometrii")
        }

    @Test
    fun skipsBrokenRoomCategory() =
        runBlocking {
            val dao = FakeFloorPlanDao()
            dao.copy(rooms = { _, floor -> ServerReply.Ok(plan(floor)) }).download(C13)
            dao.rooms.replaceAll { if (it.floor == "P0") it.copy(category = "GARAGE") else it }
            val expected = mapOf("P1" to floorSummaries())
            assertEquals(expected, dao.copy().roomFacts(C13), "Zepsute piętro powinno zostać pominięte")
        }

    private fun floorSummaries(): List<RoomSummary> =
        listOf(
            RoomSummary(RoomCategory.ELEVATOR, "elevator"),
            RoomSummary(RoomCategory.CORRIDOR, "corridor"),
        )

    private fun FakeFloorPlanDao.copy(
        floors: suspend (String) -> ServerReply<FloorList> = { ServerReply.Ok(FLOORS) },
        rooms: suspend (String, String) -> ServerReply<RoomList> = { _, _ -> ServerReply.Offline },
    ): FloorPlans =
        FloorPlanCopy(dao = this, fetchFloors = floors, fetchRooms = rooms, ioContext = EmptyCoroutineContext) { 42 }

    private fun plan(floor: String): RoomList =
        RoomList(
            source = SOURCE,
            floorOutline = AREA,
            rooms =
                listOf(
                    room("$floor.1", RoomCategory.ELEVATOR, "101").copy(liftId = "w1", liftFloors = listOf("P0", "P1")),
                    room("$floor.2", RoomCategory.CORRIDOR, null),
                ),
        )

    private fun room(id: String, category: RoomCategory, number: String?): Room =
        Room(
            id = id,
            number = number,
            function = category.name.lowercase(),
            category = category,
            geometry = AREA,
            labelPoint = point(17.05 to 51.05),
            isLabelled = number != null,
        )

    private companion object {
        const val SOURCE = "Politechnika Wrocławska"
        const val C13 = "C-13"
        val FLOORS = FloorList(source = SOURCE, floors = listOf(floor(0, "Parter"), floor(1, "1 piętro")))
        val EMPTY = RoomList(SOURCE, GeoMultiPolygon(emptyList()), emptyList())
        val AREA =
            GeoMultiPolygon(
                listOf(GeoPolygon(listOf(listOf(17.0 to 51.0, 17.1 to 51.0, 17.1 to 51.1, 17.0 to 51.0).map(::point)))),
            )

        fun floor(order: Int, label: String): Floor = Floor(id = "P$order", label = label, order = order)

        fun point(lonLat: Pair<Double, Double>): GeoPoint = GeoPoint(longitude = lonLat.first, latitude = lonLat.second)
    }
}

/** Tabele kopii planów w pamięci; `replace` zastępuje cały budynek jak transakcja w Room. */
private class FakeFloorPlanDao : FloorPlanDao {
    val plans = mutableListOf<FloorPlanEntity>()
    val rooms = mutableListOf<FloorRoomEntity>()

    override suspend fun floors(building: String): List<FloorPlanEntity> =
        plans.filter { it.building == building }.sortedBy { it.order }

    override suspend fun plan(building: String, floor: String): FloorPlanEntity? =
        plans.firstOrNull { it.building == building && it.floor == floor }

    override suspend fun rooms(building: String, floor: String): List<FloorRoomEntity> =
        rooms.filter { it.building == building && it.floor == floor }.sortedBy { it.id }

    override suspend fun roomFacts(building: String): List<FloorRoomFact> =
        rooms
            .filter { it.building == building }
            .sortedWith(compareBy({ it.floor }, { it.id }))
            .map { FloorRoomFact(floor = it.floor, category = it.category, function = it.function) }

    override suspend fun deleteRooms(building: String) {
        rooms.removeAll { it.building == building }
    }

    override suspend fun deletePlans(building: String) {
        plans.removeAll { it.building == building }
    }

    override suspend fun insertPlans(plans: List<FloorPlanEntity>) {
        this.plans += plans
    }

    override suspend fun insertRooms(rooms: List<FloorRoomEntity>) {
        this.rooms += rooms
    }
}
