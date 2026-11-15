/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Granice terenów PWr z pliku geometrii w tabeli campus_areas. Migracja powtarzalna: działa
 * przy pierwszym starcie i po zmianie pliku albo wersji importu, a tabela po niej ma dokładnie
 * tereny z pliku. Geometria z pliku trafia do bazy bez przeliczania, jako wielokąt złożony.
 *
 * <p>Wymagania: US4.9, BR-G5</p>
 *
 * @since 0.1.0
 */
final class OsmAreas implements JavaMigration {

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

    /**
     * Plik granic kopiowany przez Maven z data/areas.
     */
    private final Resource file;

    OsmAreas(final ObjectMapper mapper, final Resource file) {
        this.mapper = mapper;
        this.file = file;
    }

    @Override
    public @Nullable MigrationVersion getVersion() {
        return null;
    }

    @Override
    public String getDescription() {
        return "tereny PWr";
    }

    @Override
    public Integer getChecksum() {
        return OsmFile.checksum(this.content(), OsmAreas.FORMAT);
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(final Context context) throws SQLException {
        final Connection connection = context.getConnection();
        try (Statement clear = connection.createStatement()) {
            clear.executeUpdate("DELETE FROM campus_areas");
        }
        OsmAreas.save(connection, this.mapper.readTree(this.content()).required("features"));
    }

    private static void save(final Connection connection, final JsonNode features) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(OsmAreas.INSERT)) {
            for (final JsonNode feature : features) {
                insert.setString(1, feature.required("properties").required("name").asString());
                insert.setString(2, feature.required("geometry").toString());
                insert.addBatch();
            }
            insert.executeBatch();
        }
    }

    private byte[] content() {
        try {
            return this.file.getContentAsByteArray();
        } catch (final IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }
}
