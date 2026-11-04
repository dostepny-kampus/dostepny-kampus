/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

/**
 * Grupa barier, wartości jak schemat BarrierGroup w OpenAPI (US4.3, BR-8).
 *
 * @since 0.1.0
 */
public enum BarrierGroup {

    /** Wejście i drzwi. */
    ENTRANCE_AND_DOORS,

    /** Komunikacja pionowa. */
    VERTICAL_CIRCULATION,

    /** Komunikacja pozioma. */
    HORIZONTAL_CIRCULATION,

    /** Toalety. */
    TOILETS,

    /** Orientacja i informacja. */
    ORIENTATION_AND_INFORMATION,

    /** Inne. */
    OTHER
}
