/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Podpowiedź budynku dla punktu, zgodna ze schematem LocateResult.
 *
 * <p>Brak {@code buildingSymbol} oznacza punkt poza budynkiem (BR-G4).</p>
 *
 * @param buildingSymbol Budynek w konturze albo najbliższy w promieniu podpowiedzi
 * @param insideOutline Czy punkt leży w konturze tego budynku
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LocateResult(String buildingSymbol, boolean insideOutline) {
}
