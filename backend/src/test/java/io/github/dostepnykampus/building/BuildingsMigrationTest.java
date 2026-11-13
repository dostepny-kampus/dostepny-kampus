/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Migracja V1 zakłada tabelę budynków, w której serwer
 * przechowuje symbol, nazwę i kontur budynku do wyszukiwania na mapie.
 *
 * <p>Wymagania: US1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class BuildingsMigrationTest {

    @Test
    void usOneAppliesBuildingsMigration(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Flyway nie wykonał migracji V1 z budynkami",
            context.getBean(JdbcClient.class).sql(
                "SELECT success FROM flyway_schema_history WHERE version = '1'"
            ).query(Boolean.class).single(),
            Matchers.is(true)
        );
    }

    @Test
    void usOneStoresOutlineAsGeographicMultiPolygon(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontur budynku nie jest wielokątem złożonym w EPSG:4326",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT type || ' ' || srid FROM geometry_columns
                WHERE f_table_name = 'buildings' AND f_geometry_column = 'outline'
                """
            ).query(String.class).single(),
            Matchers.equalTo("MULTIPOLYGON 4326")
        );
    }

    @Test
    void usOneAllowsBuildingWithoutName(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kolumna nazwy budynku nie dopuszcza braku nazwy (NULL)",
            context.getBean(JdbcClient.class).sql(
                """
                SELECT is_nullable FROM information_schema.columns
                WHERE table_name = 'buildings' AND column_name = 'name'
                """
            ).query(String.class).single(),
            Matchers.equalTo("YES")
        );
    }

    @Test
    void usOneIndexesOutlinesWithGist(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Kontury budynków nie mają indeksu przestrzennego GiST",
            context.getBean(JdbcClient.class).sql(
                "SELECT indexdef FROM pg_indexes WHERE indexname = 'buildings_outline_gist'"
            ).query(String.class).single(),
            Matchers.containsString("USING gist (outline)")
        );
    }

    @Test
    void usOneSavesAndReadsBuildingOutline(final ApplicationContext context) {
        final String outline = String.join(
            "",
            "{\"type\":\"MultiPolygon\",\"coordinates\":[[[",
            "[17.0583,51.1092],[17.0591,51.1092],[17.0591,51.1097],",
            "[17.0583,51.1097],[17.0583,51.1092]",
            "]]]}"
        );
        new SampleBuilding("TEST-1", "Budynek testowy", "DETAILED")
            .geoJson(context.getBean(JdbcClient.class), outline);
        MatcherAssert.assertThat(
            "Odczytany kontur budynku różni się od zapisanego",
            context.getBean(JdbcClient.class).sql(
                "DELETE FROM buildings WHERE symbol = 'TEST-1' RETURNING ST_AsGeoJSON(outline)"
            ).query(String.class).single(),
            Matchers.equalTo(outline)
        );
    }
}
