/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path;

import java.util.List;

/**
 * Zapisane odcinki ścieżek dostosowanych.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * <p>TODO(#3486): operacja listAccessiblePaths czyta odcinki przez ten port.</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface AccessiblePaths {

    /**
     * Wszystkie odcinki.
     *
     * @return Odcinki według identyfikatora; pusta lista przed pierwszym importem
     */
    List<AccessiblePath> all();
}
