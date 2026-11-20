/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.AccessiblePaths;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Odcinki ścieżek dostosowanych z bazy jako linie GeoJSON. Transakcja testu cofa odcinki, więc
 * nie zostają dla innych testów.
 *
 * <p>Wymagania: US1.4, NFR-40</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisAccessiblePathsIT {

    @Test
    void readsPathsByIdAsGeoJsonLines(final ApplicationContext context) {
        context.getBean(JdbcClient.class).sql(
            """
            DELETE FROM accessible_paths;
            INSERT INTO accessible_paths (id, line) VALUES
                ('b', 'SRID=4326;LINESTRING(7 8,9 10)'),
                ('a', 'SRID=4326;LINESTRING(1 2,3 4,5 6)')
            """
        ).update();
        MatcherAssert.assertThat(
            "Odcinki nie są według identyfikatora albo linia nie jest GeoJSON z bazy",
            context.getBean(AccessiblePaths.class).all().stream()
                .map(path -> String.join(" ", path.id(), path.line()))
                .toList(),
            Matchers.contains(
                "a {\"type\":\"LineString\",\"coordinates\":[[1,2],[3,4],[5,6]]}",
                "b {\"type\":\"LineString\",\"coordinates\":[[7,8],[9,10]]}"
            )
        );
    }

    @Test
    void givesNoPathsForEmptyTable(final ApplicationContext context) {
        context.getBean(JdbcClient.class).sql("DELETE FROM accessible_paths").update();
        MatcherAssert.assertThat(
            "Pusta tabela odcinków nie daje pustej listy",
            context.getBean(AccessiblePaths.class).all(),
            Matchers.<AccessiblePath>empty()
        );
    }
}
