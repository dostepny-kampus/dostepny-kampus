/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Dane budynków dla scenariuszy postępu.
 *
 * @since 0.1.0
 */
final class ProgressFixtures {

    /** Dostęp do danych testowych. */
    private final JdbcClient jdbc;

    ProgressFixtures(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    static String key(final String symbol) {
        return "SP-".concat(symbol);
    }

    void building(final String symbol, final int floors) {
        this.jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES (:symbol, :symbol, CASE WHEN :amount = 0 THEN 'BASIC' ELSE 'DETAILED' END,
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM')
            """
        ).param("symbol", ProgressFixtures.key(symbol))
            .param("amount", floors).update();
        this.jdbc.sql(
            """
            INSERT INTO floors (id, building_symbol, label, sort_order)
            SELECT CAST(n AS text), :symbol, CAST(n AS text), n
            FROM generate_series(1, :amount) AS n
            """
        ).param("symbol", ProgressFixtures.key(symbol)).param("amount", floors).update();
    }

    void markedFloors(final String symbol, final int covered) {
        for (int floor = 1; floor <= covered; ++floor) {
            this.marker(symbol, Integer.toString(floor));
        }
    }

    void marker(final String symbol, final String floor) {
        this.jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author)
            VALUES (:id, :building, NULLIF(:floor, ''), 'konto-testowe')
            """
        ).param("id", UUID.randomUUID()).param("building", ProgressFixtures.key(symbol))
            .param("floor", floor).update();
    }

    void submissions(final String symbol, final String status, final int amount) {
        this.submissions(
            status, amount,
            String.format(
                "{\"kind\":\"QUICK\",\"location\":{\"coordinates\":[17,51]}%s}",
                ProgressFixtures.buildingJson(symbol)
            )
        );
    }

    UUID markerAt(final String symbol, final int floor) {
        return this.jdbc.sql(
            """
            SELECT id FROM inspection_markers
            WHERE building_symbol = :symbol AND floor_id = :floor AND NOT revoked
            """
        ).param("symbol", ProgressFixtures.key(symbol)).param("floor", Integer.toString(floor))
            .query(UUID.class).single();
    }

    private static String buildingJson(final String symbol) {
        String property = "";
        if (!symbol.isEmpty()) {
            property = String.format(",\"buildingSymbol\":\"%s\"", ProgressFixtures.key(symbol));
        }
        return property;
    }

    private void submissions(final String status, final int amount, final String body) {
        for (int index = 0; index < amount; ++index) {
            this.jdbc.sql(
                """
                INSERT INTO submissions (id, status, content)
                VALUES (:id, :status, CAST(:body AS jsonb))
                """
            ).param("id", UUID.randomUUID()).param("status", status).param("body", body).update();
        }
    }
}
