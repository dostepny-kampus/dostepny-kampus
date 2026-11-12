/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.barriers

import io.github.dostepnykampus.core.database.BARRIER_CACHE_SINGLE_ROW
import io.github.dostepnykampus.core.database.BarrierCacheDao
import io.github.dostepnykampus.core.database.BarrierCacheEntity
import io.github.dostepnykampus.core.model.BarrierType
import io.github.dostepnykampus.core.model.GeoPoint
import io.github.dostepnykampus.core.model.Severity
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionContent
import io.github.dostepnykampus.core.model.SubmissionFloor
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.model.SubmissionStatus
import io.github.dostepnykampus.core.network.ApiResult
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.IOException
import java.time.Instant
import kotlin.coroutines.EmptyCoroutineContext

/** Kopia barier w bazie: zapis przy udanej odpowiedzi serwera i czytanie, gdy serwer nie odpowie (NFR-10). */
class AcceptedBarriersCopyTest {
    private val dao = FakeBarrierCacheDao()

    /** Wiersz: bez sieci bariery przychodzą z kopii ze wszystkimi polami (NFR-10). */
    @Test
    fun nfr10_offline_uses_copy() =
        runBlocking {
            val typed = submission(id = "z1", content = precise(type = BarrierType.THRESHOLD.code, symbol = "A-1"))
            val bare = submission(id = "z2", content = precise(type = null, symbol = null)).copy(verifiedAt = null)
            val online = source { ApiResult.Success(listOf(typed, bare)) }.barriers()
            val offline = source { OFFLINE }.barriers()

            assertEquals(online, offline)
            assertEquals(listOf("z1", "z2"), offline.map(AcceptedBarrier::id))
            assertEquals(SYNCED, dao.entity?.syncedAt)
        }

    /** Wiersz: arkusz bariery, miejsce i zdjęcie także z kopii; starsza kopia bez tych pól dalej się czyta (US1.5). */
    @Test
    fun us1_5_place_and_photos() =
        runBlocking {
            val floor = SubmissionFloor(floorId = "p0", number = 0, isMezzanine = true)
            val placed = precise(type = null, symbol = "A-1").copy(entranceId = "w1", floor = floor)
            source { ApiResult.Success(listOf(submission(id = "z1", content = placed))) }.barriers()
            val restored = source { OFFLINE }.barriers().single()
            dao.entity = dao.entity?.copy(listJson = OLD_COPY)
            val old = source { OFFLINE }.barriers().single()

            assertEquals(Triple("w1", floor, listOf("p1")), restored.run { Triple(entranceId, this.floor, photoIds) })
            assertEquals(Triple(null, null, emptyList<String>()), old.run { Triple(entranceId, this.floor, photoIds) })
        }

    /** Wiersz: bez kopii i przy zepsutej kopii brak odpowiedzi serwera daje pustą listę (NFR-11). */
    @Test
    fun nfr11_no_copy_is_empty() =
        runBlocking {
            val without = source { OFFLINE }.barriers()
            dao.entity = BarrierCacheEntity(id = BARRIER_CACHE_SINGLE_ROW, listJson = "[{\"id\":", syncedAt = SYNCED)
            val broken = source { ApiResult.ServerError(code = 500) }.barriers()
            dao.entity = dao.entity?.copy(listJson = UNKNOWN_SEVERITY)
            val unknown = source { OFFLINE }.barriers()

            assertEquals(List(3) { emptyList<AcceptedBarrier>() }, listOf(without, broken, unknown))
        }

    /** Wiersz: udana pusta odpowiedź zastępuje kopię, więc nieprzyjęta bariera nie wraca z kopii (US1.3). */
    @Test
    fun us1_3_empty_replaces_copy() =
        runBlocking {
            val one = listOf(submission(id = "z1", content = precise(type = null, symbol = null)))
            source { ApiResult.Success(one) }.barriers()
            source { ApiResult.Success(emptyList()) }.barriers()

            assertEquals(emptyList<AcceptedBarrier>(), source { OFFLINE }.barriers())
        }

    private fun source(accepted: suspend () -> ApiResult<List<Submission>>) =
        ServerAcceptedBarriers(BarrierListCopy(dao, EmptyCoroutineContext) { SYNCED }, accepted)

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
        val OFFLINE = ApiResult.NetworkError(IOException("brak sieci"))
    }
}

private class FakeBarrierCacheDao : BarrierCacheDao {
    var entity: BarrierCacheEntity? = null

    override suspend fun get(): BarrierCacheEntity? = entity

    override suspend fun save(entity: BarrierCacheEntity) {
        this.entity = entity
    }
}
