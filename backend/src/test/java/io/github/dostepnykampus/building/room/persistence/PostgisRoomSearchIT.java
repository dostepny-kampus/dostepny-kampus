/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.building.room.RoomHit;
import io.github.dostepnykampus.building.room.RoomSearch;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Wyszukiwanie sali po numerze w zapisanych planach.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class PostgisRoomSearchIT {

    @Test
    void listsExactNumberBeforeLongerOnes(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Dokładny numer nie stoi przed dłuższymi albo kolejność budynku i piętra jest zła",
            PostgisRoomSearchIT.found(PostgisRoomSearchIT.search(context).rooms("q91")),
            Matchers.equalTo("G-971 P1 a | G-972 P0 d | G-971 P0 b | G-971 P0 c")
        );
    }

    @Test
    void searchesOnlyGivenBuilding(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Wyszukiwanie w budynku dało salę z innego budynku",
            PostgisRoomSearchIT.found(PostgisRoomSearchIT.search(context).rooms("Q91", "G-972")),
            Matchers.equalTo("G-972 P0 d")
        );
    }

    @Test
    void givesOneRoomPerNumberAndFloor(final ApplicationContext context) {
        MatcherAssert.assertThat(
            "Z pomieszczeń o jednym numerze na piętrze nie wyszło podpisane albo największe",
            PostgisRoomSearchIT.found(PostgisRoomSearchIT.search(context).rooms("Q93")),
            Matchers.equalTo("G-971 P0 e | G-971 P1 h")
        );
    }

    @Test
    void treatsWildcardsAsText(final ApplicationContext context) {
        final RoomSearch search = PostgisRoomSearchIT.search(context);
        MatcherAssert.assertThat(
            "Znak % albo _ w numerze zadziałał jak wzorzec albo brak sali dał wynik",
            String.join(
                " ", PostgisRoomSearchIT.found(search.rooms("Q9%")),
                PostgisRoomSearchIT.found(search.rooms("Q9_")),
                PostgisRoomSearchIT.found(search.rooms("Q99"))
            ).trim(),
            Matchers.emptyString()
        );
    }

    private static String found(final List<RoomHit> hits) {
        return String.join(
            " | ",
            hits.stream().map(
                hit -> String.join(" ", hit.building(), hit.floor().id(), hit.room().id())
            ).toList()
        );
    }

    private static RoomSearch search(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        for (final String symbol : List.of("G-971", "G-972")) {
            new SampleBuilding(symbol, "Budynek z planem", "DETAILED")
                .wkt(jdbc, "POLYGON((17 51,17.001 51,17.001 51.001,17 51.001,17 51))");
            jdbc.sql(
                """
                INSERT INTO floors (building_symbol, id, label, sort_order) VALUES
                    (:symbol, 'P0', 'Parter', 0), (:symbol, 'P1', '1 piętro', 1)
                """
            ).param("symbol", symbol).update();
        }
        jdbc.sql(
            """
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, category, outline,
                label_point, labelled
            )
            SELECT building, floor, id, number, 'Sala', 'CLASSROOM', shape,
                ST_PointOnSurface(shape), labelled
            FROM (VALUES
                ('G-971', 'P1', 'a', 'Q91', 0.0002, TRUE),
                ('G-971', 'P0', 'b', 'Q910', 0.0002, TRUE),
                ('G-971', 'P0', 'c', 'q91b', 0.0002, TRUE),
                ('G-972', 'P0', 'd', 'Q91', 0.0002, TRUE),
                ('G-971', 'P0', 'e', 'Q93', 0.0001, TRUE),
                ('G-971', 'P0', 'f', 'Q93', 0.0004, FALSE),
                ('G-971', 'P1', 'g', 'Q93', 0.0001, FALSE),
                ('G-971', 'P1', 'h', 'Q93', 0.0003, FALSE)
            ) AS room (building, floor, id, number, side, labelled),
            LATERAL (
                SELECT ST_Multi(ST_MakeEnvelope(17, 51, 17 + side, 51 + side, 4326)) AS shape
            ) AS form
            """
        ).update();
        return context.getBean(RoomSearch.class);
    }
}
