/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.SubmissionKind

private const val FORBIDDEN = 403

/**
 * Przyjęcie pokazanej wersji: zgłoszenie szybkie z typem i uciążliwością nadanymi przez Weryfikatora, inne bez nich.
 *
 * Wymagania: US10.3, BR-14
 *
 * @return decyzja; `null`, gdy zgłoszeniu szybkiemu brakuje typu albo uciążliwości
 */
internal fun CheckUiState.Ready.acceptance(): ReviewDecision? {
    val version = submission.version
    val type = quickType
    val severity = quickSeverity
    return when {
        submission.content.kind != SubmissionKind.QUICK -> ReviewDecision.Accept(version = version)
        type == null || severity == null -> null
        else -> ReviewDecision.AcceptQuick(version = version, barrierType = type.code, severity = severity.value)
    }
}

/**
 * Stan po wyborze typu albo uciążliwości zgłoszenia szybkiego, bez komunikatu o ich braku.
 *
 * Wymagania: US10.3
 *
 * @param type typ bariery
 * @param severity uciążliwość
 */
internal fun CheckUiState.Ready.picked(
    type: BarrierType? = quickType,
    severity: Severity? = quickSeverity,
): CheckUiState.Ready =
    copy(quickType = type, quickSeverity = severity, notice = notice.takeUnless { it == CheckNotice.TYPE_MISSING })

/**
 * Stan R2 po odpowiedzi serwera bez danych: brak sieci, brak uprawnień albo błąd serwera.
 *
 * Wymagania: NFR-50
 */
internal fun ServerReply<*>.checkFailure(): CheckUiState =
    when (this) {
        ServerReply.Offline -> CheckUiState.Offline
        is ServerReply.Fault -> if (code == FORBIDDEN) CheckUiState.Forbidden else CheckUiState.ServerError
        is ServerReply.Ok, ServerReply.Missing -> CheckUiState.ServerError
    }

/**
 * Adres pierwszego zdjęcia podobnego zgłoszenia [near] według jego identyfikatora.
 *
 * Wymagania: US10.1, BR-13
 *
 * @return para identyfikatora zgłoszenia i adresu zdjęcia; `null` bez zdjęć
 */
internal fun Reviews.firstPhotoOf(near: SimilarSubmission): Pair<String, String>? {
    val id = near.submission.id
    val photos = near.submission.content.photoIds
    return photos.firstOrNull()?.let { photo -> id to photoUrl(submissionId = id, photoId = photo) }
}
