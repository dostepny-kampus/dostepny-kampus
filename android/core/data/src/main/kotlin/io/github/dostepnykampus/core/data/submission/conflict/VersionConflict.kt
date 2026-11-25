/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.data.submission.conflict

import io.github.dostepnykampus.core.model.submission.Submission
import io.github.dostepnykampus.core.model.submission.SubmissionContent

/**
 * Pole treści zgłoszenia, którym mogą się różnić dwie wersje.
 *
 * Wymagania: US9.3
 *
 * @property valueIn wartość pola w treści; pola bez wartości i z wartością `false` są sobie równe
 */
public enum class ConflictField(internal val valueIn: (SubmissionContent) -> Any?) {
    /** Miejsce bariery z dokładnością pomiaru i oznaczeniem korekty ręcznej. */
    LOCATION({ listOf(it.location, it.accuracyMeters, it.isManualCorrection == true) }),

    /** Zdjęcia, bez względu na kolejność. */
    PHOTOS({ it.photoIds.toSet() }),

    /** Budynek. */
    BUILDING({ it.buildingSymbol }),

    /** Piętro. */
    FLOOR({ it.floor }),

    /** Grupa barier zgłoszenia szybkiego. */
    BARRIER_GROUP({ it.barrierGroup }),

    /** Typ bariery. */
    BARRIER_TYPE({ it.barrierType }),

    /** Uciążliwość. */
    SEVERITY({ it.severity }),

    /** Opis. */
    DESCRIPTION({ it.description }),

    /** Oznaczenie „bariera usunięta”. */
    BARRIER_REMOVED({ it.isBarrierRemoved == true }),

    /** Wejście. */
    ENTRANCE({ it.entranceId }),

    /** Oznaczenie wejścia. */
    ENTRANCE_MARKING({ it.entranceMarking }),
}

/**
 * Dwie wersje jednego zgłoszenia po odmowie 409: zapisana w telefonie i z serwera.
 *
 * Wymagania: US9.3, BR-10
 *
 * @property id identyfikator zgłoszenia
 * @property own treść zapisana w telefonie
 * @property server zgłoszenie w wersji z serwera
 * @property differences pola, którymi wersje się różnią, w kolejności [ConflictField]
 */
public data class VersionConflict(
    val id: String,
    val own: SubmissionContent,
    val server: Submission,
    val differences: List<ConflictField>,
)

/** Pola, którymi treść [own] różni się od [server], w kolejności [ConflictField]. Wymagania: US9.3 */
internal fun differencesOf(own: SubmissionContent, server: SubmissionContent): List<ConflictField> =
    ConflictField.entries.filter { it.valueIn(own) != it.valueIn(server) }
