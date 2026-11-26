/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.core.model.submission

/**
 * Pole treści zgłoszenia, którym mogą się różnić dwie wersje.
 *
 * Wymagania: US9.3, US10.8
 *
 * @property valueIn wartości pola w treści; pola bez wartości i z wartością `false` są sobie równe
 */
public enum class SubmissionField(internal val valueIn: (SubmissionContent) -> List<Any?>) {
    /** Miejsce bariery z dokładnością pomiaru i oznaczeniem korekty ręcznej. */
    LOCATION({ listOf(it.location, it.accuracyMeters, it.isManualCorrection == true) }),

    /** Zdjęcia, bez względu na kolejność. */
    PHOTOS({ listOf(it.photoIds.toSet()) }),

    /** Budynek. */
    BUILDING({ listOf(it.buildingSymbol) }),

    /** Piętro. */
    FLOOR({ listOf(it.floor) }),

    /** Grupa barier zgłoszenia szybkiego. */
    BARRIER_GROUP({ listOf(it.barrierGroup) }),

    /** Typ bariery. */
    BARRIER_TYPE({ listOf(it.barrierType) }),

    /** Uciążliwość. */
    SEVERITY({ listOf(it.severity) }),

    /** Opis. */
    DESCRIPTION({ listOf(it.description) }),

    /** Oznaczenie „bariera usunięta”. */
    BARRIER_REMOVED({ listOf(it.isBarrierRemoved == true) }),

    /** Wejście. */
    ENTRANCE({ listOf(it.entranceId) }),

    /** Oznaczenie wejścia. */
    ENTRANCE_MARKING({ listOf(it.entranceMarking) }),
}

/**
 * Pola, którymi treść [current] różni się od [previous], w kolejności [SubmissionField].
 *
 * Wymagania: US9.3, US10.8, BR-3
 *
 * @param previous wcześniejsza treść zgłoszenia
 * @param current późniejsza treść tego samego zgłoszenia
 */
public fun fieldsChanged(previous: SubmissionContent, current: SubmissionContent): List<SubmissionField> =
    SubmissionField.entries.filter { it.valueIn(previous) != it.valueIn(current) }
