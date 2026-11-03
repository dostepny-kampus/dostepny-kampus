/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import tools.jackson.databind.JsonNode;

/**
 * Wejścia z pliku konturów OpenStreetMap w tabeli entrances (US17.1, BR-24).
 *
 * <p>Oznaczenie pochodzi z tagu wheelchair: yes i designated dają
 * ACCESSIBLE, no daje INACCESSIBLE, a każda inna wartość i brak tagu
 * NO_DATA, tak jak w aplikacji. Wejście z kilkoma symbolami ma wiersz
 * dla każdego budynku. Ponowny import zmienia położenie, opis i rodzaj,
 * ale oznaczenia z obchodu (WALKTHROUGH) nie rusza (US17.3).</p>
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
        try (PreparedStatement insert = connection.prepareStatement(OsmEntrances.UPSERT)) {
            for (final JsonNode feature : this.features) {
                final JsonNode props = feature.required("properties");
                for (final JsonNode symbol : props.required("symbols")) {
                    insert.setString(1, props.required("osm_id").asString());
                    insert.setString(2, symbol.asString());
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
