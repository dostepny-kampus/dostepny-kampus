/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import androidx.work.ListenableWorker
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.inspection.MarkerChange
import io.github.dostepnykampus.core.network.inspection.MarkerChangeResult
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException

/**
 * Kolejka znaczników piętra obejrzanego wysyła się sama, gdy zadanie synchronizacji ruszy po powrocie sieci.
 * Serwer w pamięci ma budynek A-1 z 5 piętrami, z których 3 są oznaczone jako obejrzane.
 *
 * Wymagania: US15.3, BR-9, BR-20, NFR-6
 */
class MarkerSyncTest {
    /**
     * Przykład „oznaczy kolejne piętro jako obejrzane”: bez sieci zadanie prosi o ponowienie, po powrocie sieci
     * znacznik dociera na serwer bez działania Zbierającego.
     * Scenariusz (częściowo): postep-inwentaryzacji.feature: Po powrocie sieci znacznik i jego cofnięcie
     *   wysyłają się same
     */
    @Test
    fun sendsMarkerAfterReconnect() =
        runBlocking {
            val phone = MarkerPhone()
            phone.markers.mark(buildingSymbol = "A-1", floorId = "p4")
            phone.server.isOnline = false
            assertEquals(ListenableWorker.Result.retry(), phone.sync.syncResult(), "bez sieci")
            assertEquals(3, phone.server.marked.size, "postęp przed powrotem sieci")

            phone.server.isOnline = true
            assertEquals(ListenableWorker.Result.success(), phone.sync.syncResult(), "po powrocie sieci")
            assertEquals(4, phone.server.marked.size, "postęp na serwerze")
            assertEquals(emptyList<PendingMarkerChange>(), phone.markers.pending().first(), "kolejka w telefonie")
        }

    /**
     * Przykład „cofnie swój znacznik jednego z obejrzanych pięter”.
     * Scenariusz (częściowo): postep-inwentaryzacji.feature: Po powrocie sieci znacznik i jego cofnięcie
     *   wysyłają się same
     */
    @Test
    fun sendsRevokeAfterReconnect() =
        runBlocking {
            val phone = MarkerPhone()
            phone.markers.revoke(markerId = "m2", buildingSymbol = "A-1", floorId = "p2")
            phone.server.isOnline = false
            assertEquals(ListenableWorker.Result.retry(), phone.sync.syncResult(), "bez sieci")

            phone.server.isOnline = true
            assertEquals(ListenableWorker.Result.success(), phone.sync.syncResult(), "po powrocie sieci")
            assertEquals(setOf("m1", "m3"), phone.server.marked, "postęp na serwerze")
            assertEquals(emptyList<PendingMarkerChange>(), phone.markers.pending().first(), "kolejka w telefonie")
        }

    /** Awaria serwera działa jak brak sieci: zmiana zostaje w kolejce, zadanie prosi o ponowienie. */
    @ParameterizedTest
    @ValueSource(ints = [500, 503])
    fun retriesOnServerFailure(code: Int) {
        runBlocking {
            val phone = MarkerPhone()
            phone.markers.mark(buildingSymbol = "A-1", floorId = "p4")
            phone.server.failure = ApiResult.ServerError(code)
            assertEquals(ListenableWorker.Result.retry(), phone.sync.syncResult(), "awaria $code")
            assertEquals(1, phone.queue.rows.size, "zmiana czeka")
        }
    }

    /** Odmowa bez zalogowania kończy zadanie bez ponawiania, a zmiana czeka w kolejce. */
    @Test
    fun keepsQueueOnUnauthorized() =
        runBlocking {
            val phone = MarkerPhone()
            phone.markers.mark(buildingSymbol = "A-1", floorId = "p4")
            phone.server.failure = ApiResult.ServerError(401)
            assertEquals(ListenableWorker.Result.success(), phone.sync.syncResult(), "odmowa 401")
            assertEquals(1, phone.queue.rows.size, "zmiana czeka")
        }

    /** Zmiana odrzucona przez serwer zostaje w kolejce; przyjęta znika. */
    @Test
    fun keepsRejectedChange() =
        runBlocking {
            val phone = MarkerPhone()
            phone.markers.mark(buildingSymbol = "A-1", floorId = "p4")
            phone.server.rejected += "id-2"
            phone.markers.mark(buildingSymbol = "A-1", floorId = "p5")
            assertEquals(ListenableWorker.Result.success(), phone.sync.syncResult(), "wysyłka")
            assertEquals(setOf("id-2"), phone.queue.rows.keys, "w kolejce tylko odrzucona")
        }

    /** Paczka ma najwyżej 100 operacji, kolejne idą w następnej, w kolejności zapisu. */
    @Test
    fun sendsInBatchesOfHundred() =
        runBlocking {
            val phone = MarkerPhone()
            repeat(times = 101) { phone.markers.mark(buildingSymbol = "A-1", floorId = "p$it") }
            assertEquals(ListenableWorker.Result.success(), phone.sync.syncResult(), "wysyłka")
            assertEquals(listOf(100, 1), phone.server.batches, "rozmiary paczek")
            assertEquals(104, phone.server.marked.size, "postęp na serwerze")
        }
}

/** Telefon z kolejką znaczników i wysyłką oraz serwer w pamięci: budynek A-1, obejrzane 3 z 5 pięter. */
private class MarkerPhone {
    val queue = FakeMarkerQueue()
    private val ids = generateSequence(1) { it + 1 }.map { "id-$it" }.iterator()
    val markers = floorMarkersOf(queue = queue, newId = { ids.next() }, nowMillis = { queue.rows.size + 1L })
    val server = MarkerServer(marked = mutableSetOf("m1", "m2", "m3"))
    val sync = MarkerSync(queue = queue, push = server::push)
}

/** Serwer znaczników w pamięci: zbiór znaczników, które nie są cofnięte, i odpowiedź `POST /sync/push`. */
private class MarkerServer(val marked: MutableSet<String>) {
    var isOnline = true
    var failure: ApiResult<Nothing>? = null
    val rejected = mutableSetOf<String>()
    val batches = mutableListOf<Int>()

    fun push(changes: List<MarkerChange>): ApiResult<List<MarkerChangeResult>> {
        val error = failure ?: ApiResult.NetworkError(IOException("brak sieci")).takeUnless { isOnline }
        if (error != null) return error
        batches += changes.size
        val results = changes.map { MarkerChangeResult(operationId = it.operationId, isApplied = apply(it)) }
        return ApiResult.Success(results)
    }

    private fun apply(change: MarkerChange): Boolean {
        if (change.operationId in rejected) return false
        when (change) {
            is MarkerChange.Mark -> marked += change.markerId
            is MarkerChange.Revoke -> marked -= change.markerId
        }
        return true
    }
}
