/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

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
 * Wejścia z pliku konturów OpenStreetMap trafiają
 * do tabeli entrances z oznaczeniem z tagu wheelchair (ADR-0008).
 *
 * <p>Wymagania: US17.1, BR-24</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class OsmEntrancesIT {

    @Test
    void marksInaccessible(final WebApplicationContext context) {
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
    void storesEntranceRef(final WebApplicationContext context) {
        MatcherAssert.assertThat(
            "Wejścia P-23 nie mają opisu W-1 z tagu ref",
            context.getBean(JdbcClient.class).sql(
                "SELECT name FROM entrances WHERE building_symbol = 'P-23' ORDER BY name"
            ).query(String.class).list(),
            Matchers.hasItem("W-1")
        );
    }

    @Test
    void sharesEntrance(final WebApplicationContext context) {
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
    void keepsWalkthrough(final WebApplicationContext context)
        throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql(
            """
            UPDATE entrances SET marking = 'ACCESSIBLE', marking_source = 'WALKTHROUGH'
            WHERE building_symbol = 'B-8'
            """
        ).update();
        OsmEntrancesIT.reimport(context);
        MatcherAssert.assertThat(
            "Ponowny import zmienił oznaczenie wejścia z obchodu",
            jdbc.sql("SELECT marking FROM entrances WHERE building_symbol = 'B-8'")
                .query(String.class).single(),
            Matchers.equalTo("ACCESSIBLE")
        );
    }

    @Test
    void keepsPwrEntranceData(final WebApplicationContext context) throws Exception {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        jdbc.sql(
            """
            UPDATE entrances SET name = 'Wejście Cesarskie', marking = 'ACCESSIBLE', pwr = TRUE,
                approach = 'Od ul. Wybrzeże.', door_width_cm = 120
            WHERE building_symbol = 'B-8'
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, name, main, location, marking,
                marking_date, marking_source, pwr)
            VALUES ('pwr/u3qcnhb5j0', 'B-8', 'Wejście', FALSE,
                ST_SetSRID(ST_MakePoint(17.06, 51.11), 4326), 'INACCESSIBLE', DATE '2026-11-01',
                'IMPORT', TRUE)
            """
        ).update();
        OsmEntrancesIT.reimport(context);
        MatcherAssert.assertThat(
            "Ponowny import OpenStreetMap usunął albo nadpisał dane wejść z planów PWr",
            jdbc.sql(
                """
                SELECT name || '|' || marking || '|' || COALESCE(approach, '-') || '|'
                    || COALESCE(door_width_cm::text, '-')
                FROM entrances WHERE building_symbol = 'B-8' AND pwr ORDER BY id
                """
            ).query(String.class).list(),
            Matchers.contains(
                "Wejście Cesarskie|ACCESSIBLE|Od ul. Wybrzeże.|120",
                "Wejście|INACCESSIBLE|-|-"
            )
        );
    }

    private static void reimport(final WebApplicationContext context) throws Exception {
        final Connection connection = DataSourceUtils.getConnection(
            context.getBean(DataSource.class)
        );
        context.getBean("osmBuildings", FileMigration.class).migrate(
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
    }
}
