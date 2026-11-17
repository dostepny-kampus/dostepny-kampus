/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerDao
import io.github.dostepnykampus.core.database.inspection.InspectionMarkerEntity
import io.github.dostepnykampus.core.model.inspection.InspectionMarker
import io.github.dostepnykampus.core.model.inspection.MarkerPlace
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.sync.SyncPull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.IOException
import java.time.Instant

/**
 * Znaczniki pięter z `GET /sync/pull` w bazie telefonu: pobranie, zapis, cofnięcie i strumień budynku.
 *
 * Wymagania: US15.1, US15.2, BR-20
 */
class ServerMarkersTest {
    @TempDir
    lateinit var dir: File

    private val table = FakeMarkerTable()
    private val asked = mutableListOf<String?>()
    private val replies = ArrayDeque<ApiResult<SyncPull>>()

    @Test
    fun savesMarkersOfBuilding() =
        withMarkers { markers ->
            replies += pulled(cursor = "k1", marker(id = "m1", floor = "p1", isMine = true), marker(id = "m2"))
            assertEquals(ServerReply.Ok(Unit), markers.refresh(), "wynik pobrania")
            val expected = listOf(marker(id = "m1", floor = "p1", isMine = true), marker(id = "m2"))
            assertEquals(expected, markers.building("A-1").first(), "znaczniki A-1")
            assertEquals(emptyList<InspectionMarker>(), markers.building("B-1").first(), "znaczniki B-1")
        }

    @Test
    fun asksForChangesSinceLastCursor() =
        withMarkers { markers ->
            replies += pulled(cursor = "k1", marker(id = "m1"))
            replies += pulled(cursor = "k2", marker(id = "m1", isRevoked = true), marker(id = "m3", floor = "p2"))
            markers.refresh()
            markers.refresh()
            assertEquals(listOf(null, "k1"), asked, "kursory kolejnych pobrań")
            assertEquals(listOf(marker(id = "m3", floor = "p2")), markers.building("A-1").first(), "po cofnięciu")
        }

    @Test
    fun keepsCopyAndCursorWhenOffline() =
        withMarkers { markers ->
            replies += pulled(cursor = "k1", marker(id = "m1"))
            replies += ApiResult.NetworkError(IOException("brak sieci"))
            replies += pulled(cursor = "k2")
            markers.refresh()
            assertEquals(ServerReply.Offline, markers.refresh(), "wynik bez sieci")
            assertEquals(listOf(marker(id = "m1")), markers.building("A-1").first(), "kopia bez sieci")
            markers.refresh()
            assertEquals(listOf(null, "k1", "k1"), asked, "kursor po błędzie sieci")
        }

    private fun withMarkers(test: suspend (ServerMarkers) -> Unit) =
        runBlocking {
            val job = Job()
            val state = PreferenceDataStoreFactory.create(scope = CoroutineScope(job)) { File(dir, "s.preferences_pb") }
            val pull: suspend (String?) -> ApiResult<SyncPull> = { since ->
                asked += since
                replies.removeFirst()
            }
            test(SyncedMarkers(markers = table, state = state, pull = pull))
            job.cancelAndJoin()
        }

    private fun pulled(cursor: String, vararg markers: InspectionMarker): ApiResult<SyncPull> =
        ApiResult.Success(SyncPull(cursor = cursor, inspectionMarkers = markers.toList()))

    private fun marker(
        id: String,
        floor: String? = null,
        isMine: Boolean = false,
        isRevoked: Boolean = false,
    ): InspectionMarker =
        InspectionMarker(
            id = id,
            buildingSymbol = "A-1",
            place = floor?.let { MarkerPlace.Floor(it) } ?: MarkerPlace.WholeBuilding,
            createdAt = Instant.parse("2026-11-17T10:00:00Z"),
            isRevoked = isRevoked,
            isMine = isMine,
        )

    private class FakeMarkerTable : InspectionMarkerDao {
        private val rows = MutableStateFlow(emptyMap<String, InspectionMarkerEntity>())

        override fun observe(buildingSymbol: String): Flow<List<InspectionMarkerEntity>> =
            rows.map { all -> all.values.filter { it.buildingSymbol == buildingSymbol } }

        override suspend fun clear() {
            rows.value = emptyMap()
        }

        override suspend fun upsert(markers: List<InspectionMarkerEntity>) {
            rows.value += markers.associateBy { it.id }
        }

        override suspend fun delete(ids: List<String>) {
            rows.value -= ids.toSet()
        }
    }
}
