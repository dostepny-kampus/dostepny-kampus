/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barrier

import io.github.dostepnykampus.core.data.cache.FakeServerCopyDao
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.database.cache.BARRIERS_COPY
import io.github.dostepnykampus.core.database.cache.ServerCopyEntity
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionFloor
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kopia barier w bazie: zapis przy udanej odpowiedzi serwera i czytanie, gdy serwer nie odpowie. Wymagania: NFR-10 */
class AcceptedBarriersCopyTest {
    private val dao = FakeServerCopyDao()

    /** Wiersz: bez sieci bariery przychodzą z kopii ze wszystkimi polami. Wymagania: NFR-10 */
    @Test
    fun usesCopyOffline() =
        runBlocking {
            val typed = submission(id = "z1", content = precise(type = BarrierType.THRESHOLD.code, symbol = "A-1"))
            val bare = submission(id = "z2", content = precise(type = null, symbol = null)).copy(verifiedAt = null)
            val online = source { ServerReply.Ok(listOf(typed, bare)) }.barriers()
            val offline = source { OFFLINE }.barriers()

            assertEquals(online, offline, "kopia offline równa odpowiedzi serwera")
            assertEquals(listOf("z1", "z2"), offline.map(AcceptedBarrier::id), "identyfikatory z kopii offline")
            assertEquals(SYNCED, dao.rows[BARRIERS_COPY]?.syncedAt, "czas synchronizacji zapisany z kopią")
        }

    /** Wiersz: źródło podaje chwilę pobrania barier z odpowiedzi i z kopii; bez obu jej nie ma. Wymagania: NFR-10 */
    @Test
    fun keepsSyncTime() =
        runBlocking {
            val none = source { OFFLINE }.apply { barriers() }.syncedAt()
            val one = listOf(submission(id = "z1", content = precise(type = null, symbol = null)))
            val online = source { ServerReply.Ok(one) }.apply { barriers() }.syncedAt()
            val offline = source { OFFLINE }.apply { barriers() }.syncedAt()

            val synced = Instant.ofEpochMilli(SYNCED)
            assertEquals(
                listOf(null, synced, synced),
                listOf(none, online, offline),
                "brak kopii, potem dane z serwera i z kopii",
            )
        }

    /**
     * Wiersz: arkusz bariery, miejsce i zdjęcie także z kopii; starsza kopia bez tych pól dalej się czyta.
     *
     * Wymagania: US1.5
     */
    @Test
    fun keepsPlaceAndPhotos() =
        runBlocking {
            val floor = SubmissionFloor(floorId = "p0", number = 0, isMezzanine = true)
            val placed = precise(type = null, symbol = "A-1").copy(entranceId = "w1", floor = floor)
            source { ServerReply.Ok(listOf(submission(id = "z1", content = placed))) }.barriers()
            val restored = source { OFFLINE }.barriers().single()
            dao.save(dao.rows.getValue(BARRIERS_COPY).copy(documentJson = OLD_COPY))
            val old = source { OFFLINE }.barriers().single()

            assertEquals(
                Triple("w1", floor, listOf("p1")),
                restored.run { Triple(entranceId, this.floor, photoIds) },
                "zapisane pola wejścia i zdjęć wracają, stara kopia ma puste",
            )
            assertEquals(
                Triple(null, null, emptyList<String>()),
                old.run { Triple(entranceId, this.floor, photoIds) },
                "stara kopia bez pól wejścia i zdjęć",
            )
        }

    /** Wiersz: bez kopii i przy zepsutej kopii brak odpowiedzi serwera daje pustą listę. Wymagania: NFR-11 */
    @Test
    fun givesEmptyWithoutCopy() =
        runBlocking {
            val without = source { OFFLINE }.barriers()
            dao.save(ServerCopyEntity(key = BARRIERS_COPY, documentJson = "[{\"id\":", syncedAt = SYNCED))
            val broken = source { ServerReply.Fault(code = 500) }.barriers()
            dao.save(dao.rows.getValue(BARRIERS_COPY).copy(documentJson = UNKNOWN_SEVERITY))
            val unknown = source { OFFLINE }.barriers()

            assertEquals(
                List(3) { emptyList<AcceptedBarrier>() },
                listOf(without, broken, unknown),
                "brak, uszkodzenie i nieznana wersja dają pustą listę",
            )
        }

    /** Wiersz: udana pusta odpowiedź zastępuje kopię, więc nieprzyjęta bariera nie wraca z kopii. Wymagania: US1.3 */
    @Test
    fun replacesCopyWithEmpty() =
        runBlocking {
            val one = listOf(submission(id = "z1", content = precise(type = null, symbol = null)))
            source { ServerReply.Ok(one) }.barriers()
            source { ServerReply.Ok(emptyList()) }.barriers()

            assertEquals(
                emptyList<AcceptedBarrier>(),
                source { OFFLINE }.barriers(),
                "offline bez kopii daje pustą listę",
            )
        }

    private fun source(accepted: suspend () -> ServerReply<List<Submission>>) =
        ServerAcceptedBarriers(barrierCopy(dao, EmptyCoroutineContext), accepted) { SYNCED }

    private fun precise(type: String?, symbol: String?) =
        SubmissionContent(
            kind = SubmissionKind.PRECISE,
            location = GeoPoint(longitude = 17.06, latitude = 51.107),
            photoIds = listOf("p1"),
            buildingSymbol = symbol,
            barrierType = type,
            severity = Severity.BLOCKING.value,
        )

    private fun submission(id: String, content: SubmissionContent) =
        Submission(
            id = id,
            version = 1,
            status = SubmissionStatus.ACCEPTED,
            createdAt = VERIFIED,
            content = content,
            verifiedAt = VERIFIED,
        )

    private companion object {
        const val SYNCED = 42L
        const val OLD_COPY = """[{"id":"z","longitude":1.0,"latitude":2.0,"severity":3}]"""
        const val UNKNOWN_SEVERITY = """[{"id":"z","longitude":1.0,"latitude":2.0,"severity":9}]"""
        val VERIFIED: Instant = Instant.parse("2026-10-14T10:00:00Z")
        val OFFLINE = ServerReply.Offline
    }
}
