/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submissions

import io.github.dostepnykampus.core.database.OwnSubmissionEntity
import io.github.dostepnykampus.core.database.PendingSubmissionEntity
import io.github.dostepnykampus.core.model.Submission
import io.github.dostepnykampus.core.model.SubmissionKind
import io.github.dostepnykampus.core.network.ApiResult
import io.github.dostepnykampus.core.network.SubmissionEdit

private const val HTTP_NOT_FOUND = 404
private const val HTTP_CONFLICT = 409

/** Czy zgłoszenie trafia na listę C3; szybkie zgłoszenie Odwiedzającego nie trafia (US4.4, US8). */
internal fun PendingSubmissionEntity.isOwnHistory(): Boolean = kind != SubmissionKind.QUICK.name

/** Wiersz listy C3 ze stanem „W telefonie” dla zgłoszenia zapisanego w kolejce (US8, NFR-5). */
internal fun PendingSubmissionEntity.toOwnSubmissionEntity(): OwnSubmissionEntity =
    OwnSubmissionEntity(
        id = id,
        savedAtMillis = savedAtMillis,
        kind = kind,
        buildingSymbol = buildingSymbol,
        floorNumber = floorNumber,
        floorId = floorId,
        barrierType = barrierType,
        severity = severity,
    )

/** Wersja z serwera po przyjęciu treści; `null`, gdy serwer nie przyjął treści (BR-10). */
internal fun versionOf(result: ApiResult<*>): Int? {
    val value = (result as? ApiResult.Success)?.value
    return when (value) {
        is Submission -> value.version
        is SubmissionEdit.Saved -> value.submission.version
        else -> null
    }
}

/** Wersja z serwera z odpowiedzi 409 na edycję; `null` przy innym wyniku (US9.3, BR-10). */
internal fun conflictVersionOf(result: ApiResult<*>): Int? {
    val conflict = (result as? ApiResult.Success)?.value as? SubmissionEdit.Conflict
    return conflict?.run { server.version }
}

/** Kod odpowiedzi HTTP odmowy do pokazania przy „Nie wysłano”; `null`, gdy serwer nie odmówił. */
internal fun refusalCodeOf(result: ApiResult<*>): Int? =
    when (result) {
        is ApiResult.ServerError -> result.code
        ApiResult.NotFound -> HTTP_NOT_FOUND
        is ApiResult.Success -> HTTP_CONFLICT.takeIf { result.value is SubmissionEdit.Conflict }
        is ApiResult.NetworkError -> null
    }
