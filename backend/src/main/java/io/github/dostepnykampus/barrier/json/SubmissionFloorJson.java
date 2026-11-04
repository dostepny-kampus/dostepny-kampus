/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import org.jspecify.annotations.Nullable;

/**
 * Piętro zgłoszenia, schemat SubmissionFloor w OpenAPI.
 *
 * @param floorId Identyfikator piętra albo null
 * @param number Numer piętra albo null
 * @param mezzanine Półpiętro albo null, gdy nie podano
 * @since 0.1.0
 */
record SubmissionFloorJson(
    @Nullable String floorId, @Nullable Integer number, @Nullable Boolean mezzanine
) {
}
