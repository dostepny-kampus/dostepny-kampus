/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import org.flywaydb.core.api.MigrationVersion;
import org.flywaydb.core.api.migration.Context;
import org.flywaydb.core.api.migration.JavaMigration;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.Resource;
import tools.jackson.databind.ObjectMapper;

/**
 * Budynki i wejścia z pliku konturów OpenStreetMap w tabelach buildings i entrances
 * (ADR-0008). Migracja powtarzalna: działa przy pierwszym starcie i po zmianie pliku albo
 * wersji importu. Treść pliku czyta {@link OsmFile}. Istniejący budynek dostaje nowy kontur,
 * nazwę i atrybucję, a poziom szczegółowości zostaje.
 *
 * <p>Wymagania: US1, US17.1, US2</p>
 *
 * @since 0.1.0
 */
final class OsmBuildings implements JavaMigration {

    /**
     * Zapis budynku: nowy symbol dodaje wiersz, znany symbol zmienia kontur,
     * nazwę i atrybucję, a poziom szczegółowości zostaje.
     */
    static final String UPSERT = """
        INSERT INTO buildings (symbol, name, outline, source)
        VALUES (?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), ?)
        ON CONFLICT (symbol) DO UPDATE SET name = EXCLUDED.name,
            outline = EXCLUDED.outline, source = EXCLUDED.source
        """;

    /**
     * Wersja importu w sumie kontrolnej: zmiana wykonuje import ponownie
     * także bez zmiany pliku; 5 bierze nazwę z pola display_name.
     */
    static final int FORMAT = 5;

    /**
     * Parser pliku GeoJSON.
     */
    private final ObjectMapper mapper;

    /**
     * Plik konturów kopiowany przez Maven z data/buildings.
     */
    private final Resource file;

    OsmBuildings(final ObjectMapper mapper, final Resource file) {
        this.mapper = mapper;
        this.file = file;
    }

    @Override
    public @Nullable MigrationVersion getVersion() {
        return null;
    }

    @Override
    public String getDescription() {
        return "budynki z OpenStreetMap";
    }

    @Override
    public Integer getChecksum() {
        return OsmFile.checksum(this.content(), OsmBuildings.FORMAT);
    }

    @Override
    public boolean canExecuteInTransaction() {
        return true;
    }

    @Override
    public void migrate(final Context context) throws SQLException {
        final OsmFile osm = new OsmFile(this.mapper, this.mapper.readTree(this.content()));
        OsmBuildings.save(context.getConnection(), osm.buildings(), osm.attribution());
        new OsmEntrances(osm.entrances()).save(context.getConnection());
    }

    private static void save(
        final Connection connection,
        final Iterable<ImportedBuilding> buildings,
        final String source
    ) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(OsmBuildings.UPSERT)) {
            for (final ImportedBuilding building : buildings) {
                insert.setString(1, building.symbol());
                insert.setString(2, building.name());
                insert.setString(3, building.outline());
                insert.setString(4, source);
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
