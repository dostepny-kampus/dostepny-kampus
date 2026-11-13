/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import org.jspecify.annotations.Nullable;

/**
 * Budynek PWr na mapie kampusu.
 *
 * <p>Wymagania: US1</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako tekst GeoJSON
 * @since 0.1.0
 */
public record Building(
    String symbol,
    @Nullable String name,
    String detailLevel,
    String outline
) {
}
