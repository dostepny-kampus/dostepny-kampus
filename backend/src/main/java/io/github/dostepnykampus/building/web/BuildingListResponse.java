/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.BuildingList;
import java.util.List;
import tools.jackson.databind.ObjectMapper;

/**
 * Lista budynków w odpowiedzi HTTP, zgodna ze schematem BuildingList.
 *
 * @param source Atrybucja konturów budynków
 * @param buildings Budynki posortowane według symbolu
 * @since 0.1.0
 */
public record BuildingListResponse(String source, List<BuildingResponse> buildings) {

    /**
     * Buduje odpowiedź z listy budynków.
     *
     * @param list Lista budynków
     * @param mapper Parser konturów GeoJSON
     * @return Odpowiedź w kolejności listy
     */
    public static BuildingListResponse of(final BuildingList list, final ObjectMapper mapper) {
        return new BuildingListResponse(
            list.source(),
            list.buildings().stream().map(item -> BuildingResponse.of(item, mapper)).toList()
        );
    }
}
