/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.submission

import io.github.dostepnykampus.core.model.StaffRole
import java.time.Instant

/**
 * Zgłoszenie zapisane na serwerze (`Submission` z kontraktu API).
 *
 * Wymagania: BR-3, BR-5, BR-10, BR-16, BR-17
 *
 * @property id identyfikator UUID nadany w telefonie
 * @property version wersja zgłoszenia, od 1; rośnie po każdej zapisanej edycji
 * @property status status na serwerze
 * @property createdAt chwila odbioru zgłoszenia przez serwer
 * @property content treść ustalona przez składającego
 * @property rejectionReason powód odrzucenia widoczny dla autora; `null` poza odrzuceniem
 * @property verifiedAt chwila przyjęcia; `null` przed przyjęciem
 * @property author autor widziany przez Weryfikatora; `null`, gdy serwer go nie podał
 */
public data class Submission(
    val id: String,
    val version: Int,
    val status: SubmissionStatus,
    val createdAt: Instant,
    val content: SubmissionContent,
    val rejectionReason: String? = null,
    val verifiedAt: Instant? = null,
    val author: SubmissionAuthor? = null,
) {
    init {
        require(version >= 1) { "Wersja zgłoszenia zaczyna się od 1, a jest $version" }
    }
}

/**
 * Autor zgłoszenia widziany przez Weryfikatora: pseudonim i role pracownicze, bez danych osobowych.
 *
 * Wymagania: BR-5
 *
 * @property pseudonym pseudonim autora, np. „Autor 17”
 * @property staffRoles role pracownicze autora; pusty zbiór dla Odwiedzającego
 */
public data class SubmissionAuthor(val pseudonym: String, val staffRoles: Set<StaffRole>)

/** Status zgłoszenia na serwerze (`SubmissionStatus`). Status „lokalne” istnieje tylko w telefonie. Wymagania: US8.1 */
public enum class SubmissionStatus {
    /** Wysłane: serwer ma zgłoszenie, Weryfikator go jeszcze nie wziął. */
    SENT,

    /** W weryfikacji: Weryfikator wziął zgłoszenie. */
    IN_REVIEW,

    /** Przyjęte: zgłoszenie jest na mapie i na karcie budynku. Wymagania: BR-6 */
    ACCEPTED,

    /** Odrzucone z powodem; autor może je poprawić. Wymagania: BR-16 */
    REJECTED,

    /** Archiwalne: zastąpione przyjętą zmianą. Wymagania: BR-18 */
    ARCHIVED,
}
