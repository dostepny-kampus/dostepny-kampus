/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.ObjectMapper;

/**
 * Budynki i wejścia z pliku konturów OpenStreetMap w tabelach buildings i entrances
 * (ADR-0008), zapisywane przez {@link FileMigration}. Treść pliku czyta {@link OsmFile}.
 * Istniejący budynek dostaje nowy kontur, nazwę i atrybucję, a poziom szczegółowości zostaje.
 * Po zapisie dziennik INFO podaje liczbę budynków i wejść.
 *
 * <p>Wymagania: US1, US17.1, US2, NFR-57</p>
 *
 * @since 0.1.0
 */
final class OsmBuildings implements FileImport {

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
     * Dziennik importu.
     */
    private static final Logger LOG = LoggerFactory.getLogger(OsmBuildings.class);

    /**
     * Parser pliku GeoJSON.
     */
    private final ObjectMapper mapper;

    OsmBuildings(final ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(final Connection connection, final byte[] content) throws SQLException {
        final OsmFile osm = new OsmFile(this.mapper, this.mapper.readTree(content));
        final List<ImportedBuilding> buildings = osm.buildings();
        final List<ImportedEntrance> entrances = osm.entrances();
        OsmBuildings.upsert(connection, buildings, osm.attribution());
        new OsmEntrances(entrances).save(connection);
        OsmBuildings.LOG.info(
            "Import budynków z pliku: {} budynków, {} wejść", buildings.size(), entrances.size()
        );
    }

    private static void upsert(
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
}
