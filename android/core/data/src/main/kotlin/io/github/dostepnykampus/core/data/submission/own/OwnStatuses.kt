/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.own

import io.github.dostepnykampus.core.database.submission.OwnSubmissionDao
import io.github.dostepnykampus.core.database.submission.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.submission.OwnSubmissionState
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult

/** Wynik odświeżenia statusów własnych zgłoszeń z serwera. Wymagania: US8.2, US9.1, NFR-50 */
public enum class StatusRefresh {
    /** Statusy z serwera są w historii telefonu. */
    DONE,

    /** Brak sieci: lista zostaje ze statusami z ostatniej synchronizacji. Wymagania: US9.1 */
    OFFLINE,

    /** Serwer nie podał listy; zgłoszenia zostają bezpieczne w telefonie (PW-10). Wymagania: NFR-5 */
    FAILED,
}

/** Odświeżenie statusów listy C3 z serwera. Wymagania: US8.2 */
public fun interface OwnStatuses {
    /** Pobiera własne zgłoszenia z serwera i zapisuje ich status, powód odrzucenia i wersję w historii. */
    public suspend fun refresh(): StatusRefresh
}

/**
 * Statusy z [mine] (`GET /submissions?mine=true`) w historii [history] konta [subject]. Zgłoszenie, którego telefon
 * nie zna, np. po ponownej instalacji, dochodzi do listy jako wysłane; szybkie zgłoszenia lista pomija. Stan wysyłki
 * z telefonu zostaje, więc poprawka czekająca w kolejce nadal jest „W telefonie”. Wiersz innego konta zostaje.
 *
 * Wymagania: US4.4, BR-9, NFR-33
 */
internal fun ownStatusesOf(
    mine: suspend () -> ApiResult<List<Submission>>,
    history: OwnSubmissionDao,
    subject: () -> String?,
): OwnStatuses =
    OwnStatuses {
        val owner = subject() ?: return@OwnStatuses StatusRefresh.FAILED
        when (val result = mine()) {
            is ApiResult.Success -> {
                result.value.filter { it.content.kind != SubmissionKind.QUICK }.forEach { history.keep(it, owner) }
                StatusRefresh.DONE
            }

            is ApiResult.NetworkError -> {
                StatusRefresh.OFFLINE
            }

            is ApiResult.ServerError, ApiResult.NotFound -> {
                StatusRefresh.FAILED
            }
        }
    }

private suspend fun OwnSubmissionDao.keep(submission: Submission, owner: String) {
    val existing = find(submission.id)
    if (existing != null && existing.ownerSub != owner) return
    if (existing == null) save(submission.toOwnSubmissionEntity(owner))
    saveServerStatus(
        id = submission.id,
        status = submission.status.name,
        reason = submission.rejectionReason,
        version = submission.version,
    )
}

internal fun Submission.toOwnSubmissionEntity(owner: String): OwnSubmissionEntity =
    OwnSubmissionEntity(
        id = id,
        savedAtMillis = createdAt.toEpochMilli(),
        kind = content.kind.name,
        buildingSymbol = content.buildingSymbol,
        floorNumber = content.floor?.number,
        floorId = content.floor?.floorId,
        barrierType = content.barrierType,
        severity = content.severity,
        state = OwnSubmissionState.SENT,
        ownerSub = owner,
    )
