/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Schemat PostGIS przechowuje piętra i pomieszczenia budynków.
 *
 * <p>Wymagania: US16, US18</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class FloorPlanMigrationTest {

    @Test
    void createsTablesInBuildingHistory(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        MatcherAssert.assertThat(
            "Migracja planów nie figuruje w historii modułu budynków",
            jdbc.sql("""
                SELECT success FROM flyway_schema_history_building
                WHERE script = 'V1__pietra_i_pomieszczenia.sql'
                """).query(Boolean.class).single(),
            Matchers.is(true)
        );
    }

    @Test
    void storesRoomGeometryAndAllowsMissingNumber(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        MatcherAssert.assertThat(
            "Kontur pomieszczenia nie jest wielokątem złożonym w EPSG:4326",
            jdbc.sql("""
                SELECT type || ' ' || srid FROM geometry_columns
                WHERE f_table_name = 'rooms' AND f_geometry_column = 'outline'
                """).query(String.class).single(),
            Matchers.equalTo("MULTIPOLYGON 4326")
        );
        MatcherAssert.assertThat(
            "Brak numeru pomieszczenia jest niedopuszczalny",
            jdbc.sql("""
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'rooms' AND column_name = 'number'
                """).query(String.class).single(),
            Matchers.equalTo("YES")
        );
    }
}
