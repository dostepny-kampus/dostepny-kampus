/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.PostgisTestConfiguration;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Testy US1: migracja V1 zakłada tabelę budynków, w której serwer
 * przechowuje symbol, nazwę i kontur budynku do wyszukiwania na mapie.
 *
 * @since 0.1.0
 */
@SpringBootTest
@Import(PostgisTestConfiguration.class)
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
        context.getBean(JdbcClient.class).sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source)
            VALUES ('A-1', 'Gmach Główny', 'DETAILED',
                ST_SetSRID(ST_GeomFromGeoJSON(:outline), 4326),
                '© OpenStreetMap contributors')
            """
        ).param("outline", outline).update();
        MatcherAssert.assertThat(
            "Odczytany kontur budynku różni się od zapisanego",
            context.getBean(JdbcClient.class).sql(
                "SELECT ST_AsGeoJSON(outline) FROM buildings WHERE symbol = 'A-1'"
            ).query(String.class).single(),
            Matchers.equalTo(outline)
        );
    }
}
