/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.inspection

import androidx.work.ListenableWorker
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeDao
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeEntity
import io.github.dostepnykampus.core.database.inspection.PendingMarkerChangeType
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.inspection.MarkerChange
import io.github.dostepnykampus.core.network.inspection.MarkerChangeResult
import kotlinx.coroutines.flow.first
import java.net.HttpURLConnection.HTTP_INTERNAL_ERROR

/**
 * Wysyłka kolejki zmian znaczników piętra obejrzanego z telefonu.
 *
 * Wymagania: US15.3, BR-9, BR-20, NFR-6
 *
 * @param queue kolejka zmian w bazie telefonu
 * @param push wysłanie paczki operacji na serwer
 */
internal class MarkerSync(
    private val queue: PendingMarkerChangeDao,
    private val subject: () -> String?,
    private val push: suspend (String, List<MarkerChange>) -> ApiResult<List<MarkerChangeResult>>,
) {
    /**
     * Wysyła zmiany od najstarszej, po [PUSH_LIMIT] w paczce. Zmianę przyjętą przez serwer usuwa z kolejki,
     * odrzuconą zostawia. Bez sieci i przy 5xx kończy do kolejnej próby; inna odmowa, np. 401, kończy wysyłkę
     * bez ponawiania, a zmiany czekają w kolejce.
     *
     * @return `false`, gdy trzeba spróbować później
     */
    suspend fun sendAll(): Boolean {
        val owner = subject() ?: return true
        val changes = queue.observe().first().filter { it.ownerSub == owner }
        return changes.chunked(PUSH_LIMIT).all { subject() != owner || send(owner, it) }
    }

    private suspend fun send(owner: String, changes: List<PendingMarkerChangeEntity>): Boolean {
        val result = push(owner, changes.map { it.toOperation() })
        if (result is ApiResult.Success) {
            result.value
                .filter { it.isApplied }
                .forEach { queue.delete(it.operationId) }
        }
        return when (result) {
            is ApiResult.Success -> true
            is ApiResult.NetworkError -> false
            is ApiResult.ServerError -> result.code < HTTP_INTERNAL_ERROR
            ApiResult.NotFound -> true
        }
    }

    private fun PendingMarkerChangeEntity.toOperation(): MarkerChange =
        when (type) {
            PendingMarkerChangeType.SET -> toSet()
            PendingMarkerChangeType.REVOKE -> MarkerChange.Revoke(operationId = operationId, markerId = markerId)
        }

    private fun PendingMarkerChangeEntity.toSet(): MarkerChange =
        MarkerChange.Mark(
            operationId = operationId,
            markerId = markerId,
            buildingSymbol = buildingSymbol,
            floorId = floorId,
        )
}

/**
 * Wynik zadania synchronizacji po jednej wysyłce kolejki znaczników: `retry`, gdy [MarkerSync.sendAll] każe
 * spróbować później, inaczej `success`.
 *
 * Wymagania: NFR-6, BR-9
 */
internal suspend fun MarkerSync.syncResult(): ListenableWorker.Result =
    if (sendAll()) ListenableWorker.Result.success() else ListenableWorker.Result.retry()

private const val PUSH_LIMIT = 100
