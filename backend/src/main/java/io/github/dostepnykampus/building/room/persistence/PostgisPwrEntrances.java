/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.PlanEntrance;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Wpisy warstwy wejść PWr z piętrem planu w tabelach entrances i evacuation_exits. Wejście
 * z OpenStreetMap do 15 m od wpisu bierze dane najbliższego wpisu; wpis bez takiego wejścia staje
 * się wejściem {@code pwr/<geohash>}. Oznaczenie z obchodu zostaje. Wyjścia ewakuacyjne budynku
 * są zastępowane przy każdym zapisie.
 *
 * <p>Wymagania: BR-24, BR-27, US2.6, US17.1</p>
 *
 * @since 0.1.0
 */
final class PostgisPwrEntrances {

    /**
     * Wpisy warstwy z punktem jako geometrią.
     */
    private static final String RECORDS = """
        unnest(CAST(:floors AS TEXT[]), CAST(:points AS TEXT[]), CAST(:markings AS TEXT[]),
            CAST(:names AS TEXT[]), CAST(:approaches AS TEXT[]), CAST(:widths AS INTEGER[]),
            CAST(:exits AS BOOLEAN[]))
            AS pwr (floor, point, marking, name, approach, width, exit),
        LATERAL (SELECT ST_SetSRID(ST_GeomFromGeoJSON(pwr.point), 4326) AS spot) AS place
        """;

    /**
     * Dostęp do tabel wejść.
     */
    private final JdbcClient jdbc;

    /**
     * Wejścia PWr w tabelach wejść.
     *
     * @param jdbc Dostęp do tabel wejść
     */
    PostgisPwrEntrances(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Zapisuje wejścia i wyjścia ewakuacyjne budynku z wpisów warstwy wejść PWr.
     *
     * @param building Symbol budynku
     * @param known Identyfikatory pięter planu; wpis z innego piętra jest pomijany
     * @param entrances Wpisy warstwy wejść PWr
     */
    void save(final String building, final Set<String> known, final List<PlanEntrance> entrances) {
        this.jdbc.sql(
            """
            DELETE FROM entrances
            WHERE building_symbol = :building AND id LIKE 'pwr/%' AND marking_source = 'IMPORT'
            """
        ).param("building", building).update();
        this.bound(
            String.join(
                "",
                """
                UPDATE entrances SET pwr = TRUE, name = near.name, approach = near.approach,
                    door_width_cm = near.width,
                    marking_date = CASE WHEN entrances.marking_source = 'IMPORT'
                        AND entrances.marking <> near.marking
                        THEN CURRENT_DATE ELSE entrances.marking_date END,
                    marking = CASE WHEN entrances.marking_source = 'IMPORT'
                        THEN near.marking ELSE entrances.marking END
                FROM (
                    SELECT DISTINCT ON (osm.id) osm.id, pwr.name, pwr.approach, pwr.width,
                        pwr.marking
                    FROM entrances AS osm,
                """,
                PostgisPwrEntrances.RECORDS,
                """
                    WHERE osm.building_symbol = :building AND osm.id NOT LIKE 'pwr/%'
                        AND NOT pwr.exit AND pwr.floor = ANY(:known)
                        AND ST_DWithin(place.spot::geography, osm.location::geography, 15)
                    ORDER BY osm.id, ST_Distance(place.spot::geography, osm.location::geography)
                ) AS near
                WHERE entrances.building_symbol = :building AND entrances.id = near.id
                """
            ),
            building, known, entrances
        ).update();
        this.bound(
            String.join(
                "",
                """
                INSERT INTO entrances (id, building_symbol, name, main, location, floor_id,
                    marking, marking_date, marking_source, approach, door_width_cm, pwr)
                SELECT DISTINCT ON (id) id, :building, name, FALSE, spot, floor, marking,
                    CURRENT_DATE, 'IMPORT', approach, width, TRUE
                FROM (
                    SELECT 'pwr/' || ST_GeoHash(place.spot, 10) AS id, place.spot, pwr.floor,
                        pwr.marking, pwr.name, pwr.approach, pwr.width
                    FROM
                """,
                PostgisPwrEntrances.RECORDS,
                """
                    WHERE NOT pwr.exit AND pwr.floor = ANY(:known) AND NOT EXISTS (
                        SELECT 1 FROM entrances AS osm
                        WHERE osm.building_symbol = :building AND osm.id NOT LIKE 'pwr/%'
                            AND ST_DWithin(place.spot::geography, osm.location::geography, 15)
                    )
                ) AS record
                ORDER BY id
                ON CONFLICT (id, building_symbol) DO UPDATE SET name = EXCLUDED.name,
                    location = EXCLUDED.location, floor_id = EXCLUDED.floor_id,
                    approach = EXCLUDED.approach, door_width_cm = EXCLUDED.door_width_cm,
                    pwr = TRUE
                """
            ),
            building, known, entrances
        ).update();
        this.exits(building, known, entrances);
    }

    private void exits(
        final String building, final Set<String> known, final List<PlanEntrance> entrances
    ) {
        this.jdbc.sql("DELETE FROM evacuation_exits WHERE building_symbol = :building")
            .param("building", building).update();
        this.bound(
            String.join(
                "",
                """
                INSERT INTO evacuation_exits (id, building_symbol, floor_id, location,
                    description, door_width_cm)
                SELECT DISTINCT ON (id) id, :building, floor, spot, approach, width
                FROM (
                    SELECT 'pwr/' || ST_GeoHash(place.spot, 10) AS id, place.spot, pwr.floor,
                        pwr.marking, pwr.name, pwr.approach, pwr.width
                    FROM
                """,
                PostgisPwrEntrances.RECORDS,
                """
                    WHERE pwr.exit AND pwr.floor = ANY(:known) AND pwr.approach IS NOT NULL
                ) AS record
                ORDER BY id
                """
            ),
            building, known, entrances
        ).update();
    }

    private JdbcClient.StatementSpec bound(
        final String sql, final String building, final Set<String> known,
        final List<PlanEntrance> entrances
    ) {
        final String[] approaches = entrances.stream().map(PlanEntrance::approach)
            .toArray(String[]::new);
        final Integer[] widths = entrances.stream().map(PlanEntrance::doorWidthCm)
            .toArray(Integer[]::new);
        final Boolean[] exits = entrances.stream().map(PlanEntrance::evacuationExit)
            .toArray(Boolean[]::new);
        return this.jdbc.sql(sql).param("building", building)
            .param("known", known.toArray(String[]::new))
            .param("floors", entrances.stream().map(PlanEntrance::floorId).toArray(String[]::new))
            .param("points", entrances.stream().map(PlanEntrance::point).toArray(String[]::new))
            .param("markings", entrances.stream().map(PlanEntrance::marking).toArray(String[]::new))
            .param("names", entrances.stream().map(PlanEntrance::name).toArray(String[]::new))
            .param("approaches", approaches).param("widths", widths).param("exits", exits);
    }
}
