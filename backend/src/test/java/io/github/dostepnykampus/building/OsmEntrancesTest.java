/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import java.sql.Connection;
import javax.sql.DataSource;
import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Testy US17.1 i BR-24: wejścia z pliku konturów OpenStreetMap trafiają
 * do tabeli entrances z oznaczeniem z tagu wheelchair (ADR-0008).
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class OsmEntrancesTest {

    @Test
    void brTwentyFourMarksInaccessible(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Wejście B-8 z wheelchair=no nie jest niedostępne z importu",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT marking || ' ' || marking_source FROM entrances
                WHERE building_symbol = 'B-8'
                """
            ).query(String.class).single(),
            Matchers.equalTo("INACCESSIBLE IMPORT")
        );
    }

    @Test
    void usSeventeenStoresEntranceRef(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Wejścia P-23 nie mają opisu W-1 z tagu ref",
            context.getBean(JdbcClient.class).sql(
                "SELECT name FROM entrances WHERE building_symbol = 'P-23' ORDER BY name"
            ).query(String.class).list(),
            Matchers.hasItem("W-1")
        );
    }

    @Test
    void usSeventeenSharesEntrance(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Wejście wspólne C-1, C-3 i C-4 nie ma wiersza dla każdego budynku",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT max(shared) FROM (SELECT count(*) AS shared FROM entrances
                    WHERE id IN (SELECT id FROM entrances WHERE building_symbol = 'C-1')
                    GROUP BY id) AS ids
                """
            ).query(Integer.class).single(),
            Matchers.equalTo(3)
        );
    }

    @Test
    void usSeventeenKeepsWalkthrough(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql(
            """
            UPDATE entrances SET marking = 'ACCESSIBLE', marking_source = 'WALKTHROUGH'
            WHERE building_symbol = 'B-8'
            """
        ).update();
        final Connection connection = DataSourceUtils.getConnection(
            context.getBean(DataSource.class)
        );
        context.getBean(OsmBuildings.class).migrate(
            new Context() {
                @Override
                public Configuration getConfiguration() {
                    throw new UnsupportedOperationException("Import nie czyta konfiguracji");
                }

                @Override
                public Connection getConnection() {
                    return connection;
                }
            }
        );
        MatcherAssert.assertThat(
            "Ponowny import zmienił oznaczenie wejścia z obchodu",
            jdbc.sql("SELECT marking FROM entrances WHERE building_symbol = 'B-8'")
                .query(String.class).single(),
            Matchers.equalTo("ACCESSIBLE")
        );
    }
}
