/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

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
}
