/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Zapytanie o listę zgłoszeń, parametry GET /submissions w OpenAPI.
 *
 * @param statuses Szukane statusy, co najmniej jeden
 * @param buildingSymbol Symbol budynku albo null, gdy bez filtra
 * @param floorId Identyfikator piętra albo null, gdy bez filtra
 * @since 0.1.0
 */
public record SubmissionQuery(
    Set<SubmissionStatus> statuses, @Nullable String buildingSymbol, @Nullable String floorId
) {

    /**
     * Zapytanie z niemodyfikowalną kopią statusów. Pusty zbiór statusów kończy się
     * niekontrolowanym wyjątkiem IllegalArgumentException; brak zbioru kończy się
     * wyjątkiem NullPointerException.
     */
    public SubmissionQuery {
        statuses = Set.copyOf(statuses);
        if (statuses.isEmpty()) {
            throw new IllegalArgumentException("Zapytanie wymaga co najmniej jednego statusu");
        }
    }
}
