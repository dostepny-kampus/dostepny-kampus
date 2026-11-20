/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.application;

import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.AccessiblePaths;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Odczyt ścieżek dostosowanych dla warstwy web: kontroler nie sięga do portu
 * {@link AccessiblePaths}, tylko do tej usługi.
 *
 * <p>Wymagania: US1.4</p>
 *
 * @since 0.1.0
 */
@Service
public final class PathReading {

    /**
     * Port odczytu odcinków.
     */
    private final AccessiblePaths paths;

    PathReading(final AccessiblePaths paths) {
        this.paths = paths;
    }

    /**
     * Wszystkie odcinki ścieżek dostosowanych.
     *
     * @return Odcinki według identyfikatora; pusta lista przed pierwszym importem
     */
    public List<AccessiblePath> paths() {
        return this.paths.all();
    }
}
