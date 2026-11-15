/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.SubmissionQuery;
import io.github.dostepnykampus.barrier.submission.SubmissionStatus;
import io.github.dostepnykampus.shared.error.InvalidInput;
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
 * @param mine Tylko własne zgłoszenia: {@code true} albo {@code false}, null znaczy fałsz
 * @since 0.1.0
 */
record SubmissionFilter(
    @Nullable String buildingSymbol, @Nullable String floorId,
    @Nullable List<SubmissionStatus> status, @Nullable String mine
) {

    /**
     * Czy filtr dotyczy tylko własnych zgłoszeń; brak parametru znaczy fałsz (domyślna
     * wartość z OpenAPI). Wartość inna niż {@code true} albo {@code false} kończy się
     * wyjątkiem {@link InvalidInput}.
     *
     * @return Prawda dla mine=true
     */
    boolean own() {
        if (this.mine != null && !"true".equals(this.mine) && !"false".equals(this.mine)) {
            throw new InvalidInput("Parametr mine przyjmuje true albo false");
        }
        return "true".equals(this.mine);
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
        if (!this.own() && !statuses.stream().allMatch(SubmissionStatus::publiclyVisible)) {
            throw new InvalidInput("Bez mine=true dozwolony jest tylko status ACCEPTED");
        }
        return new SubmissionQuery(statuses, this.buildingSymbol, this.floorId);
    }

    private Set<SubmissionStatus> statuses() {
        final Set<SubmissionStatus> result;
        if (this.status == null) {
            result = Stream.of(SubmissionStatus.values())
                .filter(value -> this.own() || value.publiclyVisible())
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SubmissionStatus.class)));
        } else {
            result = Set.copyOf(this.status);
        }
        return result;
    }
}
