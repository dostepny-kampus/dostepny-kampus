/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.inspection.domain.ProgressRepository;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Odczyt aktywnych znaczników inwentaryzacji.
 *
 * <p>Wymagania: US11.1, US11.3, BR-20</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcProgress implements ProgressRepository {

    /** Dostęp do tabeli znaczników. */
    private final JdbcClient jdbc;

    JdbcProgress(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<ActiveMarker> activeMarkers() {
        return this.jdbc.sql(
            """
            SELECT id, building_symbol, floor_id, author, created_at
            FROM inspection_markers
            WHERE NOT revoked
            ORDER BY building_symbol, created_at, id
            """
        ).query(
            (rows, num) -> new ActiveMarker(
                MarkerRows.id(rows), MarkerRows.place(rows),
                MarkerRows.instant(rows, "created_at"), MarkerRows.text(rows, "author")
            )
        ).list();
    }
}
