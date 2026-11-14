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
final class FloorPlanMigrationIT {

    @Test
    void createsTablesInBuildingHistory(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Migracja planów nie figuruje w historii modułu budynków",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT success FROM flyway_schema_history_building
                WHERE script = 'V1__pietra_i_pomieszczenia.sql'
                """
            ).query(Boolean.class).single(),
            Matchers.is(true)
        );
    }

    @Test
    void storesRoomGeometry(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontur pomieszczenia nie jest wielokątem złożonym w EPSG:4326",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT type || ' ' || srid FROM geometry_columns
                WHERE f_table_name = 'rooms' AND f_geometry_column = 'outline'
                """
            ).query(String.class).single(),
            Matchers.equalTo("MULTIPOLYGON 4326")
        );
    }

    @Test
    void usSixteenStoresRoomCategoryLabelAndFloorOutline(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Brak kategorii, punktu podpisu, znacznika podpisu albo obrysu piętra w schemacie",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT table_name || '.' || column_name || ' ' || is_nullable
                FROM information_schema.columns
                WHERE (table_name = 'rooms'
                    AND column_name IN ('category', 'label_point', 'labelled'))
                    OR (table_name = 'floors' AND column_name = 'outline')
                ORDER BY 1
                """
            ).query(String.class).list(),
            Matchers.contains(
                "floors.outline YES",
                "rooms.category NO",
                "rooms.label_point NO",
                "rooms.labelled NO"
            )
        );
    }

    @Test
    void usSixteenStoresLabelPointAndFloorOutlineInWgs(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Punkt podpisu albo obrys piętra nie ma typu i układu EPSG:4326",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT f_table_name || '.' || f_geometry_column || ' ' || type || ' ' || srid
                FROM geometry_columns
                WHERE (f_table_name = 'rooms' AND f_geometry_column = 'label_point')
                    OR (f_table_name = 'floors' AND f_geometry_column = 'outline')
                ORDER BY 1
                """
            ).query(String.class).list(),
            Matchers.contains("floors.outline MULTIPOLYGON 4326", "rooms.label_point POINT 4326")
        );
    }

    @Test
    void allowsMissingRoomNumber(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Brak numeru pomieszczenia jest niedopuszczalny",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'rooms' AND column_name = 'number'
                """
            ).query(String.class).single(),
            Matchers.equalTo("YES")
        );
    }
}
