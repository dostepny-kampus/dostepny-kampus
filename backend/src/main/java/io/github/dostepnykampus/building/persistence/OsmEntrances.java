/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Wejścia z pliku konturów OpenStreetMap w tabeli entrances, odczytane przez
 * {@link OsmFile}. Ponowny import zmienia
 * położenie, opis i rodzaj, nie rusza oznaczeń z obchodu (WALKTHROUGH) ani nazwy i oznaczenia
 * wejść z planów PWr i usuwa nieaktualne powiązania wejść z oznaczeniem IMPORT spoza PWr.
 *
 * <p>Wymagania: US17.1, BR-24, US17.3</p>
 *
 * @since 0.1.0
 */
final class OsmEntrances {

    /**
     * Zapis wejścia; data oznaczenia zmienia się tylko ze zmianą oznaczenia.
     */
    static final String UPSERT = """
        INSERT INTO entrances (id, building_symbol, name, type, main, location,
            marking, marking_date, marking_source)
        VALUES (?, ?, ?, ?, ?, ST_SetSRID(ST_GeomFromGeoJSON(?), 4326), ?,
            CURRENT_DATE, 'IMPORT')
        ON CONFLICT (id, building_symbol) DO UPDATE SET
            name = CASE WHEN entrances.pwr THEN entrances.name ELSE EXCLUDED.name END,
            type = EXCLUDED.type, main = EXCLUDED.main, location = EXCLUDED.location,
            marking_date = CASE WHEN entrances.marking_source = 'IMPORT' AND NOT entrances.pwr
                AND entrances.marking <> EXCLUDED.marking
                THEN EXCLUDED.marking_date ELSE entrances.marking_date END,
            marking = CASE WHEN entrances.marking_source = 'IMPORT' AND NOT entrances.pwr
                THEN EXCLUDED.marking ELSE entrances.marking END
        """;

    /**
     * Wejścia z pliku, po jednym na parę wejście i budynek.
     */
    private final Iterable<ImportedEntrance> entrances;

    OsmEntrances(final Iterable<ImportedEntrance> entrances) {
        this.entrances = entrances;
    }

    void save(final Connection connection) throws SQLException {
        final List<String> ids = new ArrayList<>(64);
        final List<String> buildings = new ArrayList<>(64);
        try (PreparedStatement insert = connection.prepareStatement(OsmEntrances.UPSERT)) {
            for (final ImportedEntrance entrance : this.entrances) {
                ids.add(entrance.id());
                buildings.add(entrance.building());
                insert.setString(1, entrance.id());
                insert.setString(2, entrance.building());
                insert.setString(3, entrance.name());
                insert.setString(4, entrance.type());
                insert.setBoolean(5, entrance.main());
                insert.setString(6, entrance.location());
                insert.setString(7, entrance.marking());
                insert.addBatch();
            }
            insert.executeBatch();
        }
        try (
            PreparedStatement delete = connection.prepareStatement(
                """
                DELETE FROM entrances AS old
                WHERE old.marking_source = 'IMPORT' AND old.id NOT LIKE 'pwr/%'
                    AND NOT EXISTS (
                        SELECT 1 FROM unnest(CAST(? AS TEXT[]), CAST(? AS TEXT[]))
                            AS snapshot(id, building_symbol)
                        WHERE snapshot.id = old.id
                            AND snapshot.building_symbol = old.building_symbol
                    )
                """
            )
        ) {
            delete.setObject(1, ids.toArray(String[]::new));
            delete.setObject(2, buildings.toArray(String[]::new));
            delete.executeUpdate();
        }
    }
}
