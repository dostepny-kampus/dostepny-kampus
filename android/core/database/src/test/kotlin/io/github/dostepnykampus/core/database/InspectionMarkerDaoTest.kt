/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.database

import androidx.room.Room
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Kopia znaczników piętra obejrzanego z serwera w bazie telefonu. Wymagania: US15.1, US15.2 */
@RunWith(RobolectricTestRunner::class)
class InspectionMarkerDaoTest {
    private lateinit var database: DkDatabase
    private lateinit var dao: InspectionMarkerDao

    @Before
    fun open() {
        database =
            Room
                .inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), DkDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        dao = database.inspectionMarkers()
    }

    @After
    fun close() {
        database.close()
    }

    /** Odczyt zwraca tylko znaczniki wskazanego budynku, od najstarszego. */
    @Test
    fun observesOneBuilding() =
        runTest {
            dao.apply(
                saved =
                    listOf(
                        marker(id = "m2", building = "A-1", createdAt = 9),
                        marker(id = "m1", building = "A-1", createdAt = 5),
                        marker(id = "m3", building = "B-4", createdAt = 1),
                    ),
                revokedIds = emptyList(),
            )
            assertEquals(
                "znaczniki budynku A-1 w kolejności odbioru",
                listOf("m1", "m2"),
                dao.observe("A-1").first().map { it.id },
            )
        }

    /** Znacznik całego budynku ma puste piętro i wraca z odczytem bez zmian. */
    @Test
    fun keepsWholeBuildingMarker() =
        runTest {
            val whole = marker(id = "m1", building = "B-4", createdAt = 5).copy(floorId = null, isMine = false)
            dao.apply(saved = listOf(whole), revokedIds = emptyList())
            assertEquals("znacznik bez piętra", listOf(whole), dao.observe("B-4").first())
        }

    /** Ponowny zapis znacznika zastępuje poprzedni, a cofnięte znaczniki znikają. */
    @Test
    fun appliesUpsertAndRevoke() =
        runTest {
            dao.apply(saved = listOf(marker(id = "m1"), marker(id = "m2")), revokedIds = emptyList())
            dao.apply(
                saved = listOf(marker(id = "m1").copy(isMine = false)),
                revokedIds = listOf("m2", "nieznany"),
            )
            assertEquals(
                "m1 zastąpiony, m2 usunięty",
                listOf("m1" to false),
                dao.observe("A-1").first().map { it.id to it.isMine },
            )
        }

    /** Znacznik zapisany i cofnięty w jednej zmianie nie zostaje w tabeli. */
    @Test
    fun dropsRevokedInSameApply() =
        runTest {
            dao.apply(saved = listOf(marker(id = "m1")), revokedIds = listOf("m1"))
            assertEquals("brak znaczników", emptyList<InspectionMarkerEntity>(), dao.observe("A-1").first())
        }

    /** Czyszczenie usuwa znaczniki wszystkich budynków. */
    @Test
    fun clearsAllBuildings() =
        runTest {
            val saved = listOf(marker(id = "m1"), marker(id = "m2", building = "B-4"))
            dao.apply(saved = saved, revokedIds = emptyList())
            dao.clear()
            val left = dao.observe("A-1").first() + dao.observe("B-4").first()
            assertEquals("brak znaczników po czyszczeniu", emptyList<InspectionMarkerEntity>(), left)
        }

    private fun marker(id: String, building: String = "A-1", createdAt: Long = 5): InspectionMarkerEntity =
        InspectionMarkerEntity(
            id = id,
            buildingSymbol = building,
            floorId = "p1",
            createdAtMillis = createdAt,
            isMine = true,
        )
}
