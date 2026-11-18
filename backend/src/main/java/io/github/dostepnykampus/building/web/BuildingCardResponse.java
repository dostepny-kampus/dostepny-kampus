/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRawValue;
import io.github.dostepnykampus.building.domain.BuildingCard;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Karta budynku w odpowiedzi HTTP, zgodna ze schematem BuildingDetails.
 *
 * <p>Wymagania: US2, US2.6, BR-27</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON z bazy, wstawiany do odpowiedzi bez zmian
 * @param entrances Wejścia budynku; pusta lista, gdy dane nie mają wejść
 * @param evacuationExits Wyjścia ewakuacyjne budynku; pusta lista nie trafia do JSON
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingCardResponse(
    String symbol,
    @Nullable String name,
    String detailLevel,
    @JsonRawValue String outline,
    List<EntranceResponse> entrances,
    @JsonInclude(JsonInclude.Include.NON_EMPTY) List<EvacuationExitResponse> evacuationExits
) {

    /**
     * Odpowiedź z karty budynku.
     *
     * @param card Karta budynku
     */
    public BuildingCardResponse(final BuildingCard card) {
        this(
            card.symbol(), card.name(), card.detailLevel(), card.outline(),
            card.entrances().stream().map(EntranceResponse::new).toList(),
            card.evacuationExits().stream().map(EvacuationExitResponse::new).toList()
        );
    }
}
