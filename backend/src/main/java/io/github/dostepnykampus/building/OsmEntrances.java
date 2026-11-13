/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import tools.jackson.databind.JsonNode;

/**
 * Wejścia z pliku konturów OpenStreetMap w tabeli entrances. Oznaczenie z tagu wheelchair:
 * yes i designated dają ACCESSIBLE, no daje INACCESSIBLE, inna wartość i brak tagu NO_DATA.
 * Wejście z kilkoma symbolami ma wiersz dla każdego budynku. Ponowny import zmienia
 * położenie, opis i rodzaj, nie rusza oznaczeń z obchodu (WALKTHROUGH) i usuwa nieaktualne
 * powiązania wejść z oznaczeniem IMPORT.
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
        ON CONFLICT (id, building_symbol) DO UPDATE SET name = EXCLUDED.name,
            type = EXCLUDED.type, main = EXCLUDED.main, location = EXCLUDED.location,
            marking_date = CASE WHEN entrances.marking_source = 'IMPORT'
                AND entrances.marking <> EXCLUDED.marking
                THEN EXCLUDED.marking_date ELSE entrances.marking_date END,
            marking = CASE WHEN entrances.marking_source = 'IMPORT'
                THEN EXCLUDED.marking ELSE entrances.marking END
        """;

    /**
     * Wejścia z pliku, czyli obiekty z kind = entrance.
     */
    private final Iterable<JsonNode> features;

    OsmEntrances(final Iterable<JsonNode> features) {
        this.features = features;
    }

    void save(final Connection connection) throws SQLException {
        final List<String> ids = new ArrayList<>(64);
        final List<String> buildings = new ArrayList<>(64);
        try (PreparedStatement insert = connection.prepareStatement(OsmEntrances.UPSERT)) {
            for (final JsonNode feature : this.features) {
                final JsonNode props = feature.required("properties");
                final String id = props.required("osm_id").asString();
                for (final JsonNode symbol : props.required("symbols")) {
                    final String building = symbol.asString();
                    ids.add(id);
                    buildings.add(building);
                    insert.setString(1, id);
                    insert.setString(2, building);
                    insert.setString(3, props.path("ref").asString(null));
                    insert.setString(4, props.path("entrance").asString(null));
                    insert.setBoolean(5, "main".equals(props.path("entrance").asString(null)));
                    insert.setString(6, feature.required("geometry").toString());
                    insert.setString(
                        7, OsmEntrances.marking(props.path("wheelchair").asString(""))
                    );
                    insert.addBatch();
                }
            }
            insert.executeBatch();
        }
        try (
            PreparedStatement delete = connection.prepareStatement(
                """
                DELETE FROM entrances AS old
                WHERE old.marking_source = 'IMPORT'
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

    private static String marking(final String wheelchair) {
        final String result;
        if ("yes".equals(wheelchair) || "designated".equals(wheelchair)) {
            result = "ACCESSIBLE";
        } else if ("no".equals(wheelchair)) {
            result = "INACCESSIBLE";
        } else {
            result = "NO_DATA";
        }
        return result;
    }
}
