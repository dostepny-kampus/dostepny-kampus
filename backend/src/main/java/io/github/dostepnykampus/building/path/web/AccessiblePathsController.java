/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.web;

import io.github.dostepnykampus.building.path.application.PathReading;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operacja listAccessiblePaths (odcinki ścieżek dostosowanych mapy kampusu).
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@RestController
public final class AccessiblePathsController {

    /**
     * Zapisane odcinki ścieżek dostosowanych.
     */
    private final PathReading paths;

    AccessiblePathsController(final PathReading paths) {
        this.paths = paths;
    }

    /**
     * Zwraca wszystkie odcinki ścieżek dostosowanych.
     *
     * @return Odcinki z liniami GeoJSON i źródłem danych; pusta lista przed pierwszym importem
     */
    @GetMapping("/accessible-paths")
    AccessiblePathListResponse listAccessiblePaths() {
        return new AccessiblePathListResponse(this.paths.paths());
    }
}
