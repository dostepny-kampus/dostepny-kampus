/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.building.BuildingCard;
import java.util.List;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Karta budynku w odpowiedzi HTTP, zgodna ze schematem BuildingDetails.
 *
 * <p>Wymagania: US2</p>
 *
 * @param symbol Symbol budynku, np. C-13
 * @param name Nazwa budynku albo null, gdy budynek jej nie ma
 * @param detailLevel Poziom szczegółowości: DETAILED albo BASIC
 * @param outline Kontur budynku jako GeoJSON
 * @param entrances Wejścia budynku; pusta lista, gdy dane nie mają wejść
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record BuildingCardResponse(
    String symbol,
    @Nullable String name,
    String detailLevel,
    JsonNode outline,
    List<EntranceResponse> entrances
) {

    /**
     * Buduje odpowiedź z karty budynku.
     *
     * @param card Karta budynku
     * @param mapper Parser konturu i położeń GeoJSON
     * @return Odpowiedź z wejściami w kolejności karty
     */
    public static BuildingCardResponse of(final BuildingCard card, final ObjectMapper mapper) {
        return new BuildingCardResponse(
            card.symbol(), card.name(), card.detailLevel(), mapper.readTree(card.outline()),
            card.entrances().stream().map(item -> EntranceResponse.of(item, mapper)).toList()
        );
    }
}
