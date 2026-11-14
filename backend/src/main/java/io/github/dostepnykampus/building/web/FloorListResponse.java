/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.FloorList;
import java.util.List;

/**
 * Piętra budynku w odpowiedzi HTTP, zgodne ze schematem FloorList.
 *
 * <p>Wymagania: US16, NFR-40</p>
 *
 * @param source Nazwa źródła danych planów pięter
 * @param floors Piętra posortowane rosnąco według kolejności
 * @since 0.1.0
 */
public record FloorListResponse(String source, List<FloorResponse> floors) {

    /**
     * Odpowiedź z listy pięter budynku.
     *
     * @param list Lista pięter budynku
     */
    public FloorListResponse(final FloorList list) {
        this(list.source(), list.floors().stream().map(FloorResponse::new).toList());
    }
}
