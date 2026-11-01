/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Publiczny odczyt budynków kampusu, operacja listBuildings.
 *
 * @since 0.1.0
 */
@RestController
public final class BuildingsController {

    /**
     * Odczyt budynków z bazy.
     */
    private final BuildingQuery query;

    BuildingsController(final BuildingQuery query) {
        this.query = query;
    }

    /**
     * Zwraca budynki posortowane według symbolu, z konturem GeoJSON.
     *
     * @return Lista budynków z atrybucją konturów
     */
    @GetMapping("/buildings")
    BuildingList listBuildings() {
        return this.query.listBuildings();
    }
}
