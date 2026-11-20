/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.persistence;

import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.AccessiblePaths;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Odcinki ścieżek dostosowanych z tabeli accessible_paths.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisAccessiblePaths implements AccessiblePaths {

    /**
     * Dostęp do tabeli accessible_paths.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy odczyt odcinków na bazie serwera.
     *
     * @param jdbc Dostęp do tabeli accessible_paths
     */
    public PostgisAccessiblePaths(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<AccessiblePath> all() {
        return this.jdbc.sql(
            "SELECT id, ST_AsGeoJSON(line) AS line FROM accessible_paths ORDER BY id"
        ).query(
            (rows, num) -> new AccessiblePath(rows.getString("id"), rows.getString("line"))
        ).list();
    }
}
