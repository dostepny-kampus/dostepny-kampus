/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.sync.SyncPull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant

/**
 * Pobieranie znaczników pięter z `GET /sync/pull`: zapis aktywnych, usuwanie cofniętych, kursor `since`
 * i zachowanie kopii przy błędzie sieci.
 *
 * Wymagania: US15.1, US15.2, BR-20
 */
class ServerFloorMarkersTest {
    @Test
    fun pullsSavesAndRevokesMarkers() =
        runTest {
            val dao = FakeInspectionMarkerDao()
            val cursors = mutableListOf<String?>()
            val replies =
                ArrayDeque(
                    listOf(
                        ApiResult.Success(SyncPull(cursor = "c-1", inspectionMarkers = listOf(OWN, OTHER))),
                        ApiResult.Success(
                            SyncPull(cursor = "c-2", inspectionMarkers = listOf(OWN.copy(isRevoked = true))),
                        ),
                    ),
                )
            val store =
                serverFloorMarkersOf(dao = dao) { since ->
                    cursors += since
                    replies.removeFirst()
                }
            assertTrue(store.pull(), "pierwsze pobranie")
            assertEquals(listOf(OWN, OTHER), store.markers("A-1").first(), "oba znaczniki w bazie")
            assertTrue(store.pull(), "drugie pobranie")
            assertEquals(listOf(OTHER), store.markers("A-1").first(), "po cofnięciu zostaje drugi znacznik")
            assertEquals(listOf(null, "c-1"), cursors, "kursor since w kolejnych żądaniach")
        }

    /**
     * Kursor cofnięty o margines powtarza zmiany: ten sam znacznik i to samo cofnięcie przyjęte drugi raz nie
     * zmieniają kopii w telefonie.
     *
     * Wymagania: US15.1, US15.2, BR-20
     */
    @Test
    fun repeatedChangesKeepState() =
        runTest {
            val dao = FakeInspectionMarkerDao()
            val revoked = OTHER.copy(isRevoked = true)
            val replies =
                ArrayDeque(
                    listOf(
                        ApiResult.Success(SyncPull(cursor = "c-1", inspectionMarkers = listOf(OWN, OTHER))),
                        ApiResult.Success(SyncPull(cursor = "c-2", inspectionMarkers = listOf(OWN, revoked))),
                        ApiResult.Success(SyncPull(cursor = "c-3", inspectionMarkers = listOf(OWN, revoked))),
                    ),
                )
            val store = serverFloorMarkersOf(dao = dao) { replies.removeFirst() }
            store.pull()
            assertTrue(store.pull(), "pobranie z cofnięciem")
            val once = store.markers("A-1").first()

            assertTrue(store.pull(), "powtórzone zmiany przyjęte bez błędu")
            assertEquals(once, store.markers("A-1").first(), "powtórka nie zmienia kopii")
            assertEquals(listOf(OWN), once, "jeden wiersz znacznika, cofnięty usunięty")
        }

    @Test
    fun keepsStoredMarkersOnError() =
        runTest {
            val dao = FakeInspectionMarkerDao()
            val replies =
                ArrayDeque<ApiResult<SyncPull>>(
                    listOf(
                        ApiResult.Success(SyncPull(cursor = "c-1", inspectionMarkers = listOf(OWN))),
                        ApiResult.NetworkError(cause = IOException("offline")),
                    ),
                )
            val store = serverFloorMarkersOf(dao = dao) { replies.removeFirst() }
            assertTrue(store.pull(), "pierwsze pobranie")
            assertFalse(store.pull(), "błąd sieci")
            assertEquals(listOf(OWN), store.markers("A-1").first(), "poprzednia kopia zostaje")
        }

    private companion object {
        val TIME: Instant = Instant.parse("2026-11-18T10:00:00Z")
        val OWN =
            InspectionMarker(
                id = "m-1",
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor("p0"),
                createdAt = TIME,
                isRevoked = false,
                isMine = true,
            )
        val OTHER =
            InspectionMarker(
                id = "m-2",
                buildingSymbol = "A-1",
                place = MarkerPlace.Floor("p1"),
                createdAt = TIME.plusSeconds(1),
                isRevoked = false,
                isMine = false,
            )
    }
}

/** Atrapa tabeli znaczników z serwera do testów JVM. */
private class FakeInspectionMarkerDao : InspectionMarkerDao {
    private val stored = MutableStateFlow<Map<String, InspectionMarkerEntity>>(emptyMap())

    override fun observe(buildingSymbol: String): Flow<List<InspectionMarkerEntity>> =
        stored.map { map ->
            map.values
                .filter { it.buildingSymbol == buildingSymbol }
                .sortedWith(compareBy(InspectionMarkerEntity::createdAtMillis, InspectionMarkerEntity::id))
        }

    override suspend fun clear() {
        stored.value = emptyMap()
    }

    override suspend fun upsert(markers: List<InspectionMarkerEntity>) {
        stored.value = stored.value + markers.associateBy { it.id }
    }

    override suspend fun delete(ids: List<String>) {
        stored.value = stored.value - ids.toSet()
    }
}
