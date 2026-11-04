/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.web;

import io.github.dostepnykampus.InvalidInput;
import io.github.dostepnykampus.barrier.domain.SubmissionQuery;
import io.github.dostepnykampus.barrier.domain.SubmissionStatus;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Parametry zapytania GET /submissions z kontraktu API.
 *
 * @param buildingSymbol Symbol budynku albo null, gdy bez filtra
 * @param floorId Identyfikator piętra albo null, gdy bez filtra
 * @param status Szukane statusy albo null, gdy nie podano
 * @param mine Tylko własne zgłoszenia, null znaczy fałsz
 * @since 0.1.0
 */
record SubmissionFilter(
    @Nullable String buildingSymbol, @Nullable String floorId,
    @Nullable List<SubmissionStatus> status, Boolean mine
) {

    /**
     * Filtr, w którym nieobecny parametr mine oznacza fałsz (domyślna wartość z OpenAPI).
     */
    public SubmissionFilter {
        mine = Boolean.TRUE.equals(mine);
    }

    /**
     * Przenosi parametry do zapytania domeny.
     *
     * @return Zapytanie o listę zgłoszeń
     */
    SubmissionQuery toQuery() {
        if (this.status != null
            && (this.status.isEmpty() || this.status.stream().anyMatch(Objects::isNull))) {
            throw new InvalidInput("Parametr status nie może być pusty");
        }
        final Set<SubmissionStatus> statuses = this.statuses();
        if (!this.mine && !statuses.stream().allMatch(SubmissionStatus::publiclyVisible)) {
            throw new InvalidInput("Bez mine=true dozwolony jest tylko status ACCEPTED");
        }
        return new SubmissionQuery(statuses, this.buildingSymbol, this.floorId);
    }

    private Set<SubmissionStatus> statuses() {
        final Set<SubmissionStatus> result;
        if (this.status == null) {
            result = Stream.of(SubmissionStatus.values())
                .filter(SubmissionStatus::publiclyVisible)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SubmissionStatus.class)));
        } else {
            result = Set.copyOf(this.status);
        }
        return result;
    }
}
