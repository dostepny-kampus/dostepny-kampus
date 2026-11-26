/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.persistence;

import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.AccessiblePaths;
import io.github.dostepnykampus.building.path.PathStore;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Odcinki ścieżek dostosowanych w tabeli accessible_paths.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisAccessiblePaths implements AccessiblePaths, PathStore {

    /**
     * Dostęp do tabeli accessible_paths.
     */
    private final JdbcClient jdbc;

    /**
     * Transakcja zastąpienia odcinków.
     */
    private final TransactionOperations transaction;

    /**
     * Tworzy odczyt i zapis odcinków na bazie serwera.
     *
     * @param jdbc Dostęp do tabeli accessible_paths
     * @param transaction Transakcja zastąpienia odcinków
     */
    public PostgisAccessiblePaths(final JdbcClient jdbc, final TransactionOperations transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public List<AccessiblePath> all() {
        return this.jdbc.sql(
            "SELECT id, ST_AsGeoJSON(line) AS line FROM accessible_paths ORDER BY id"
        ).query(
            (rows, num) -> new AccessiblePath(rows.getString("id"), rows.getString("line"))
        ).list();
    }

    @Override
    public void replace(final List<AccessiblePath> paths) {
        this.transaction.executeWithoutResult(
            status -> {
                this.jdbc.sql("DELETE FROM accessible_paths").update();
                this.jdbc.sql(
                    """
                    INSERT INTO accessible_paths (id, line)
                    SELECT path.id, ST_SetSRID(ST_GeomFromGeoJSON(path.line), 4326)
                    FROM unnest(CAST(:ids AS TEXT[]), CAST(:lines AS TEXT[])) AS path (id, line)
                    """
                ).param("ids", paths.stream().map(AccessiblePath::id).toArray(String[]::new))
                    .param("lines", paths.stream().map(AccessiblePath::line).toArray(String[]::new))
                    .update();
            }
        );
    }
}
