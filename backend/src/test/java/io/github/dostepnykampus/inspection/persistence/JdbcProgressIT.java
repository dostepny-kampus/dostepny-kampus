/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.inspection.domain.BuildingProgress;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Odczyt postępu budynków przez {@link JdbcProgress}: piętra, zgłoszenia bez archiwalnych
 * i spoza budynku, wejścia z obchodu oraz aktywne znaczniki z pseudonimem autora.
 *
 * <p>Wymagania: US11.1, US11.2, US11.3, US11.4, BR-5, BR-20, BR-21, BR-24</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class JdbcProgressIT {

    @Test
    void readsDetailedAndBasicProgress(final WebApplicationContext context) {
        JdbcProgressIT.seed(context.getBean(JdbcClient.class));
        final List<BuildingProgress> list = context.getBean(JdbcProgress.class)
            .read("konto-testowe");
        final BuildingProgress detailed = JdbcProgressIT.find(list, "T-D1");
        final BuildingProgress basic = JdbcProgressIT.find(list, "T-B1");
        Assertions.assertEquals(
            "Floors[total=2, covered=1]/2/1/2/1/2/true/true/Whole[inspected=true]",
            String.format(
                "%s/%d/%d/%d/%d/%d/%b/%b/%s",
                detailed.coverage(),
                detailed.counts().pending(), detailed.counts().accepted(),
                detailed.counts().entrances(), detailed.counts().checked(),
                detailed.markers().size(),
                detailed.markers().getFirst().pseudonym().startsWith("Autor "),
                detailed.markers().getFirst().mine(),
                basic.coverage()
            ),
            "Odczyt postępu budynku szczegółowego albo podstawowego nie zgadza się z bazą"
        );
    }

    private static void seed(final JdbcClient jdbc) {
        jdbc.sql(
            """
            INSERT INTO buildings (symbol, name, detail_level, outline, source) VALUES
            ('T-D1', 'Det', 'DETAILED',
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM'),
            ('T-B1', 'Bas', 'BASIC',
                ST_GeomFromText('MULTIPOLYGON(((17 51, 17.01 51, 17.01 51.01, 17 51)))', 4326),
                'OSM')
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO floors (id, building_symbol, label, sort_order) VALUES
            ('f0', 'T-D1', 'Parter', 0), ('f1', 'T-D1', '1', 1)
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO inspection_markers (id, building_symbol, floor_id, author, revoked) VALUES
            ('00000000-0000-4000-8000-000000000001', 'T-D1', 'f0', 'konto-testowe', FALSE),
            ('00000000-0000-4000-8000-000000000002', 'T-D1', 'f0', 'inny', FALSE),
            ('00000000-0000-4000-8000-000000000003', 'T-D1', 'f1', 'inny', TRUE),
            ('00000000-0000-4000-8000-000000000004', 'T-B1', NULL, 'inny', FALSE)
            """
        ).update();
        jdbc.sql(
            """
            INSERT INTO submissions (id, status, kind, building_symbol, location, content) VALUES
            (:a, 'SENT', 'QUICK', 'T-D1', ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb),
            (:b, 'IN_REVIEW', 'QUICK', 'T-D1', ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb),
            (:c, 'ACCEPTED', 'QUICK', 'T-D1', ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb),
            (:d, 'ARCHIVED', 'QUICK', 'T-D1', ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb),
            (:e, 'REJECTED', 'QUICK', 'T-D1', ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb),
            (:f, 'ACCEPTED', 'QUICK', NULL, ST_GeomFromText('POINT(17 51)', 4326), '{}'::jsonb)
            """
        ).param("a", UUID.randomUUID()).param("b", UUID.randomUUID())
            .param("c", UUID.randomUUID()).param("d", UUID.randomUUID())
            .param("e", UUID.randomUUID()).param("f", UUID.randomUUID()).update();
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, marking,
                marking_date, marking_source) VALUES
            ('e1', 'T-D1', TRUE, ST_GeomFromText('POINT(17 51)', 4326), 'ACCESSIBLE',
                DATE '2026-11-10', 'WALKTHROUGH'),
            ('e2', 'T-D1', FALSE, ST_GeomFromText('POINT(17 51)', 4326), 'NO_DATA',
                DATE '2026-11-10', 'IMPORT')
            """
        ).update();
    }

    private static BuildingProgress find(final List<BuildingProgress> list, final String symbol) {
        return list.stream().filter(item -> symbol.equals(item.symbol())).findFirst()
            .orElseThrow(() -> new IllegalStateException(String.format("Brak %s", symbol)));
    }
}
