/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczyt budynków z tabeli buildings do listy na mapie (US1).
 *
 * @since 0.1.0
 */
@Service
final class BuildingQuery {

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    /**
     * Parser GeoJSON zwracanego przez ST_AsGeoJSON.
     */
    private final ObjectMapper mapper;

    BuildingQuery(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    BuildingList listBuildings() {
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level, source,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            ORDER BY symbol
            """
        ).query(this::rows);
    }

    private BuildingList rows(final ResultSet rows) throws SQLException {
        final List<Building> found = new ArrayList<>(8);
        String chosen = "© OpenStreetMap contributors";
        while (rows.next()) {
            if (found.isEmpty()) {
                chosen = BuildingQuery.text(rows, "source");
            }
            found.add(this.one(rows));
        }
        return new BuildingList(chosen, found);
    }

    private Building one(final ResultSet rows) throws SQLException {
        return new Building(
            BuildingQuery.text(rows, "symbol"),
            BuildingQuery.text(rows, "name"),
            BuildingQuery.text(rows, "detail_level"),
            this.mapper.readTree(BuildingQuery.text(rows, "outline"))
        );
    }

    private static String text(final ResultSet rows, final String column)
        throws SQLException {
        final String value = rows.getString(column);
        if (value == null) {
            throw new SQLException("Brak wartości kolumny %s".formatted(column));
        }
        return value;
    }
}
