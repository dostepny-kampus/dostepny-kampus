/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.specs;

import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

/**
 * Dane budynków dla jednego scenariusza postępu, wycofywane po jego zakończeniu.
 *
 * @since 0.1.0
 */
final class ProgressFixtures implements AutoCloseable {

    /** Dostęp do danych testowych. */
    private final JdbcClient jdbc;

    /** Transakcja danych scenariusza. */
    private final PlatformTransactionManager manager;

    /** Stan transakcji scenariusza. */
    private final TransactionStatus transaction;

    ProgressFixtures(final JdbcClient jdbc, final PlatformTransactionManager manager) {
        this.jdbc = jdbc;
        this.manager = manager;
        this.transaction = manager.getTransaction(new DefaultTransactionDefinition());
    }

    void building(final String symbol, final int floors) {
        this.jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES (:symbol, :symbol, :level,
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM')
            """
        ).param("symbol", ProgressFixtures.key(symbol))
            .param("level", floors == 0 ? "BASIC" : "DETAILED").update();
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

    UUID marker(final String symbol, final String floor) {
        final UUID id = UUID.randomUUID();
        this.jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author)
            VALUES (:id, :building, NULLIF(:floor, ''), 'konto-testowe')
            """
        ).param("id", id).param("building", ProgressFixtures.key(symbol))
            .param("floor", floor).update();
        return id;
    }

    void submissions(final String symbol, final String status, final int amount) {
        final String building = symbol.isEmpty() ? "" : String.format(
            ",\"buildingSymbol\":\"%s\"", ProgressFixtures.key(symbol)
        );
        this.submissions(status, amount, String.format(
            "{\"kind\":\"QUICK\",\"location\":{\"coordinates\":[17,51]}%s}", building
        ));
    }

    @Override
    public void close() {
        this.manager.rollback(this.transaction);
    }

    static String key(final String symbol) {
        return "SP-".concat(symbol);
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
