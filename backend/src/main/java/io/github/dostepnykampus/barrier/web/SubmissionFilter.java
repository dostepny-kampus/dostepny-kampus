/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Parametry zapytania GET /submissions z kontraktu API.
 *
 * @param buildingSymbol Symbol budynku albo null, gdy bez filtra
 * @param floorId Identyfikator piętra albo null, gdy bez filtra
 * @param status Szukane statusy albo null, gdy nie podano
 * @param mine Tylko własne zgłoszenia, domyślnie fałsz
 * @since 0.1.0
 */
record SubmissionFilter(
    @Nullable String buildingSymbol, @Nullable String floorId,
    @Nullable List<SubmissionStatus> status, boolean mine
) {

    /**
     * Przenosi parametry do zapytania domeny.
     *
     * @return Zapytanie o listę zgłoszeń
     */
    SubmissionQuery toQuery() {
        return SubmissionFilter.missing();
    }

    private static <T> T missing() {
        throw new UnsupportedOperationException();
    }
}
