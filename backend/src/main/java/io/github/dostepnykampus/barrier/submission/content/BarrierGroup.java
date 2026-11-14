/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.content;

/**
 * Grupa barier: krótkie nazwy stałych, w liczbie i kolejności
 * wartości schematu BarrierGroup w OpenAPI; wartość ze spec podaje opis stałej.
 *
 * <p>Wymagania: US4.3, BR-8</p>
 *
 * @since 0.1.0
 */
public enum BarrierGroup {

    /** Wejście i drzwi, w OpenAPI ENTRANCE_AND_DOORS. */
    ENTRANCE,

    /** Komunikacja pionowa, w OpenAPI VERTICAL_CIRCULATION. */
    VERTICAL,

    /** Komunikacja pozioma, w OpenAPI HORIZONTAL_CIRCULATION. */
    HORIZONTAL,

    /** Toalety, w OpenAPI TOILETS. */
    TOILETS,

    /** Orientacja i informacja, w OpenAPI ORIENTATION_AND_INFORMATION. */
    ORIENTATION,

    /** Inne, w OpenAPI OTHER. */
    OTHER
}
