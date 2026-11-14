/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.inspection.persistence;

import io.github.dostepnykampus.building.inspection.InspectionMarkers;
import io.github.dostepnykampus.building.inspection.NewMarker;
import java.sql.Types;
import org.springframework.jdbc.core.SqlParameterValue;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Znaczniki pięter obejrzanych w tabeli {@code inspection_markers}.
 *
 * @since 0.1.0
 */
@Component
public final class JdbcInspectionMarkers implements InspectionMarkers {

    /** Klient zapytań JDBC. */
    private final JdbcClient jdbc;

    /**
     * Konstruktor.
     *
     * @param client Klient zapytań JDBC
     */
    public JdbcInspectionMarkers(final JdbcClient client) {
        this.jdbc = client;
    }

    @Override
    public void set(final NewMarker marker, final String author) {
        this.jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author)
            VALUES (?, ?, ?, ?) ON CONFLICT (id) DO NOTHING
            """
        ).params(
            marker.id(), marker.buildingSymbol(),
            new SqlParameterValue(Types.VARCHAR, marker.floorId()), author
        ).update();
    }
}
