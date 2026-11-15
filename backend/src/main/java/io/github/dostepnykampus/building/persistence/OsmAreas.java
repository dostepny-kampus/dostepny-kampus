/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Granice terenów PWr z pliku geometrii w tabeli campus_areas, zapisywane przez
 * {@link FileMigration}; tabela po zapisie ma dokładnie tereny z pliku. Geometria z pliku
 * trafia do bazy bez przeliczania, jako wielokąt złożony.
 *
 * <p>Wymagania: US4.9, BR-G5</p>
 *
 * @since 0.1.0
 */
final class OsmAreas implements FileImport {

    /**
     * Zapis terenu: nazwa i granica z geometrii GeoJSON.
     */
    static final String INSERT = """
        INSERT INTO campus_areas (name, boundary)
        VALUES (?, ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(?), 4326)))
        """;

    /**
     * Wersja importu w sumie kontrolnej: zmiana wykonuje import ponownie
     * także bez zmiany pliku.
     */
    static final int FORMAT = 1;

    /**
     * Parser pliku GeoJSON.
     */
    private final ObjectMapper mapper;

    OsmAreas(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(final Connection connection, final byte[] content) throws SQLException {
        try (Statement clear = connection.createStatement()) {
            clear.executeUpdate("DELETE FROM campus_areas");
        }
        OsmAreas.insert(connection, this.mapper.readTree(content).required("features"));
    }

    private static void insert(
        final Connection connection, final JsonNode features
    ) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(OsmAreas.INSERT)) {
            for (final JsonNode feature : features) {
                insert.setString(1, feature.required("properties").required("name").asString());
                insert.setString(2, feature.required("geometry").toString());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }
}
