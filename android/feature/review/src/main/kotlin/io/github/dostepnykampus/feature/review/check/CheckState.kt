/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.feature.review.check

import io.github.dostepnykampus.core.data.building.ServerEntrances
import io.github.dostepnykampus.core.data.review.Reviews
import io.github.dostepnykampus.core.data.server.ServerReply
import io.github.dostepnykampus.core.model.building.Entrance
import io.github.dostepnykampus.core.model.building.isAfterDayOf
import io.github.dostepnykampus.core.model.review.ReviewDecision
import io.github.dostepnykampus.core.model.review.SimilarSubmission
import io.github.dostepnykampus.core.model.submission.BarrierType
import io.github.dostepnykampus.core.model.submission.Severity
import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent
import io.github.dostepnykampus.core.model.submission.SubmissionKind
import io.github.dostepnykampus.core.model.submission.SubmissionStatus

private const val FORBIDDEN = 403

/**
 * Poprzednie zgłoszenie, które zmiana „bariera usunięta” zastąpi.
 *
 * Wymagania: US10.8, BR-19, BR-23
 *
 * @property status status poprzedniego zgłoszenia
 * @property photoUrl adres pierwszego zdjęcia poprzedniego zgłoszenia; `null` bez zdjęć
 */
data class PreviousSubmission(val status: SubmissionStatus, val photoUrl: String?)

/**
 * Poprzednie zgłoszenie zmiany „bariera usunięta” z serwera.
 *
 * Wymagania: US10.8, BR-19
 *
 * @param content treść sprawdzanego zgłoszenia
 * @return poprzednie zgłoszenie; `null` przy innej zmianie albo gdy serwer go nie dał
 */
internal suspend fun Reviews.previousOf(content: SubmissionContent): PreviousSubmission? {
    val id = content.replacesSubmissionId?.takeIf { content.isBarrierRemoved == true } ?: return null
    val found = submission(id)
    return if (found is ServerReply.Ok) {
        val firstPhoto =
            found.value.content.photoIds
                .firstOrNull()
        val photo = firstPhoto?.let { photoUrl(submissionId = id, photoId = it) }
        PreviousSubmission(status = found.value.status, photoUrl = photo)
    } else {
        null
    }
}

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

/**
 * Obecne oznaczenie wejścia, którego dotyczy zmiana oznaczenia.
 *
 * Wymagania: US17.2, US10.8, BR-23, BR-24
 *
 * @property entrance wejście z serwera z oznaczeniem, dniem i źródłem
 * @property isChangedSinceSent czy oznaczenie przyjęto po wysłaniu zmiany
 */
data class CurrentMarking(val entrance: Entrance, val isChangedSinceSent: Boolean)

/**
 * Obecne oznaczenie wejścia ze zmiany oznaczenia [submission].
 *
 * Wymagania: US17.2, US10.8, BR-24
 *
 * @param submission sprawdzane zgłoszenie
 * @return obecne oznaczenie; `null` przy innym zgłoszeniu albo gdy serwer nie zna wejścia
 */
internal suspend fun ServerEntrances.currentMarking(submission: Submission): CurrentMarking? {
    val content = submission.content
    if (content.kind != SubmissionKind.ENTRANCE_MARKING) return null
    val symbol = content.buildingSymbol ?: return null
    val id = content.entranceId ?: return null
    val entrance = entrances(symbol).firstOrNull { it.id == id } ?: return null
    val isChanged = entrance.assessment?.isAfterDayOf(submission.createdAt) == true
    return CurrentMarking(entrance = entrance, isChangedSinceSent = isChanged)
}
