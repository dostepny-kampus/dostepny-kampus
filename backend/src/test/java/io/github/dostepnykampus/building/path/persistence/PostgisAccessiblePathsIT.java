/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.path.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.path.AccessiblePath;
import io.github.dostepnykampus.building.path.AccessiblePaths;
import io.github.dostepnykampus.building.path.PathStore;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Odcinki ścieżek dostosowanych z bazy jako linie GeoJSON; import zastępuje wszystkie odcinki.
 * Transakcja testu cofa odcinki, więc nie zostają dla innych testów.
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
    void replacesPathsWithoutDuplicatesOnSecondImport(final ApplicationContext context) {
        context.getBean(JdbcClient.class).sql(
            "INSERT INTO accessible_paths (id, line) VALUES ('stary', 'SRID=4326;LINESTRING(0 0,1 1)')"
        ).update();
        final PathStore store = context.getBean(PathStore.class);
        final List<AccessiblePath> paths = List.of(
            new AccessiblePath("a", "{\"type\":\"LineString\",\"coordinates\":[[1,2],[3,4]]}")
        );
        store.replace(paths);
        store.replace(paths);
        MatcherAssert.assertThat(
            "Ponowny import zdublował odcinki albo zostawił odcinek sprzed importu",
            context.getBean(AccessiblePaths.class).all(),
            Matchers.equalTo(paths)
        );
    }

    @Test
    void storesLineInWorldCoordinates(final ApplicationContext context) {
        context.getBean(PathStore.class).replace(
            List.of(
                new AccessiblePath("a", "{\"type\":\"LineString\",\"coordinates\":[[1,2],[3,4]]}")
            )
        );
        MatcherAssert.assertThat(
            "Zapisana linia nie ma układu WGS 84",
            context.getBean(JdbcClient.class)
                .sql("SELECT ST_SRID(line) FROM accessible_paths WHERE id = 'a'")
                .query(Integer.class).single(),
            Matchers.equalTo(4326)
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
