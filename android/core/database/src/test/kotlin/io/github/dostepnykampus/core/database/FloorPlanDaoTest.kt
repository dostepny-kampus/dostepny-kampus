/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.room.FloorPlanDao
import io.github.dostepnykampus.core.database.room.FloorPlanEntity
import io.github.dostepnykampus.core.database.room.FloorRoomEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kopia planów pięter w bazie Room: plan budynku zastępowany w całości. Wymagania: US16.1, NFR-10 */
@RunWith(RobolectricTestRunner::class)
class FloorPlanDaoTest {
    @Test
    fun readsSavedFloorsInOrder() =
        stored { dao ->
            val rooms = listOf(room("2"), room("1"), room(id = "3", floor = "P1"))
            dao.replace("C-13", listOf(plan("P1"), plan("P0")), rooms)
            assertEquals(
                listOf(listOf(plan("P0"), plan("P1")), listOf(room("1"), room("2"))),
                listOf(dao.floors("C-13"), dao.roomsOf("P0")),
            )
        }

    @Test
    fun replacesWholeBuildingPlan() =
        stored { dao ->
            dao.replace("A-1", listOf(plan(floor = "P1", building = "A-1")), emptyList())
            dao.replace("C-13", listOf(plan("P0"), plan("P1")), listOf(room("1"), room(id = "3", floor = "P1")))
            dao.replace("C-13", listOf(plan("P0")), listOf(room("4")))

            assertEquals(
                listOf(null, emptyList<FloorRoomEntity>(), listOf(room("4")), plan(floor = "P1", building = "A-1")),
                listOf(dao.planOf("P1"), dao.roomsOf("P1"), dao.roomsOf("P0"), dao.planOf("P1", building = "A-1")),
            )
        }

    private fun stored(check: suspend (FloorPlanDao) -> Unit) {
        val database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        try {
            runTest { check(database.floorPlans()) }
        } finally {
            database.close()
        }
    }

    private suspend fun FloorPlanDao.planOf(floor: String, building: String = "C-13") =
        plan(building = building, floor = floor)

    private suspend fun FloorPlanDao.roomsOf(floor: String) = rooms(building = "C-13", floor = floor)

    private fun plan(floor: String, building: String = "C-13"): FloorPlanEntity =
        FloorPlanEntity(
            building = building,
            floor = floor,
            label = "Piętro $floor",
            order = floor.drop(1).toInt(),
            source = "Politechnika Wrocławska",
            outlineJson = SQUARE,
            syncedAt = 42,
        )

    private fun room(id: String, floor: String = "P0"): FloorRoomEntity =
        FloorRoomEntity(
            building = "C-13",
            floor = floor,
            id = id,
            number = if (id == "2") null else "10$id",
            function = "Winda",
            category = "ELEVATOR",
            geometryJson = SQUARE,
            labelLongitude = 17.05,
            labelLatitude = 51.1,
            isLabelled = id != "2",
        )
}

private const val SQUARE = """{"type":"MultiPolygon","coordinates":[[[[0,0],[1,0],[1,1],[0,0]]]]}"""
