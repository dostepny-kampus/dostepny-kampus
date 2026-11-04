/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model

import java.time.Instant

/**
 * Zgłoszenie zapisane na serwerze (`Submission` z kontraktu API).
 *
 * @property id identyfikator UUID nadany w telefonie
 * @property version wersja zgłoszenia, od 1; rośnie po każdej zapisanej edycji (BR-3, BR-10)
 * @property status status na serwerze
 * @property createdAt chwila odbioru zgłoszenia przez serwer
 * @property content treść ustalona przez składającego
 * @property rejectionReason powód odrzucenia widoczny dla autora (BR-16); `null` poza odrzuceniem
 * @property verifiedAt chwila przyjęcia (BR-17); `null` przed przyjęciem
 */
data class Submission(
    val id: String,
    val version: Int,
    val status: SubmissionStatus,
    val createdAt: Instant,
    val content: SubmissionContent,
    val rejectionReason: String? = null,
    val verifiedAt: Instant? = null,
) {
    init {
        require(version >= 1) { "Wersja zgłoszenia zaczyna się od 1, a jest $version" }
    }
}

/** Status zgłoszenia na serwerze (`SubmissionStatus`, US8.1). Status „lokalne” istnieje tylko w telefonie. */
enum class SubmissionStatus {
    /** Wysłane: serwer ma zgłoszenie, Weryfikator go jeszcze nie wziął. */
    SENT,

    /** W weryfikacji: Weryfikator wziął zgłoszenie. */
    IN_REVIEW,

    /** Przyjęte: zgłoszenie jest na mapie i na karcie budynku (BR-6). */
    ACCEPTED,

    /** Odrzucone z powodem (BR-16); autor może je poprawić. */
    REJECTED,

    /** Archiwalne: zastąpione przyjętą zmianą (BR-18). */
    ARCHIVED,
}
