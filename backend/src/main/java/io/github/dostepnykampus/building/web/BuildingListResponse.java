/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.BuildingList;
import java.util.List;

/**
 * Lista budynków w odpowiedzi HTTP, zgodna ze schematem BuildingList.
 *
 * @param source Atrybucja konturów budynków
 * @param buildings Budynki posortowane według symbolu
 * @since 0.1.0
 */
public record BuildingListResponse(String source, List<BuildingResponse> buildings) {

    /**
     * Odpowiedź z listy budynków.
     *
     * @param list Lista budynków
     */
    public BuildingListResponse(final BuildingList list) {
        this(
            list.source(),
            list.buildings().stream().map(BuildingResponse::new).toList()
        );
    }
}
