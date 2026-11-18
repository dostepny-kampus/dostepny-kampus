/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.PlanEntrance;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Wpisy warstwy wejść PWr z piętrem planu w tabelach entrances i evacuation_exits. Wejście
 * z OpenStreetMap dostaje w kolumnach pwr_name, pwr_marking, approach i door_width_cm dane
 * najbliższego wpisu w promieniu 15 m, a wpis bez nazwy w uwagach nie zasłania nazwy z OSM;
 * każdy inny wpis staje się wejściem {@code pwr/<geohash>}, nie ruszając name i marking z OSM.
 * Data oznaczenia z PWr to dzień edycji warstwy. Dane PWr i wyjścia ewakuacyjne są zastępowane.
 *
 * <p>Wymagania: BR-24, BR-27, US2.6, US17.1, US17.3, BR-25</p>
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
            CAST(:exits AS BOOLEAN[])) WITH ORDINALITY
            AS pwr (floor, point, marking, name, approach, width, exit, ord),
        LATERAL (SELECT ST_SetSRID(ST_GeomFromGeoJSON(pwr.point), 4326) AS spot) AS place
        """;

    /**
     * Najbliższy wpis każdego wejścia z OpenStreetMap budynku w promieniu 15 m.
     */
    private static final String NEAREST = String.join(
        "",
        """
        WITH near AS (
            SELECT DISTINCT ON (osm.id) osm.id, pwr.ord, pwr.name, pwr.approach, pwr.width,
                pwr.marking
            FROM entrances AS osm,
        """,
        PostgisPwrEntrances.RECORDS,
        """
            WHERE osm.building_symbol = :building AND osm.id NOT LIKE 'pwr/%'
                AND NOT pwr.exit AND pwr.floor = ANY(:known)
                AND ST_DWithin(place.spot::geography, osm.location::geography, 15)
            ORDER BY osm.id, ST_Distance(place.spot::geography, osm.location::geography),
                pwr.ord
        )
        """
    );

    /**
     * Dostęp do tabel wejść.
     */
    private final JdbcClient jdbc;

    /**
     * Dzień edycji warstwy wejść albo null, gdy usługa go nie podaje.
     */
    private final @Nullable LocalDate edited;

    /**
     * Wejścia PWr w tabelach wejść.
     *
     * @param jdbc Dostęp do tabel wejść
     * @param edited Dzień edycji warstwy wejść, data oznaczenia z PWr; null to dzień zapisu
     */
    PostgisPwrEntrances(final JdbcClient jdbc, final @Nullable LocalDate edited) {
        this.jdbc = jdbc;
        this.edited = edited;
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
        this.jdbc.sql(
            """
            UPDATE entrances
            SET pwr_name = NULL, pwr_marking = NULL, approach = NULL, door_width_cm = NULL,
                pwr_marking_date = NULL
            WHERE building_symbol = :building
            """
        ).param("building", building).update();
        this.bound(
            String.join(
                "",
                PostgisPwrEntrances.NEAREST,
                """
                UPDATE entrances SET pwr_name = near.name, pwr_marking = near.marking,
                    approach = near.approach, door_width_cm = near.width,
                    pwr_marking_date = COALESCE(CAST(:edited AS DATE), CURRENT_DATE)
                FROM near
                WHERE entrances.building_symbol = :building AND entrances.id = near.id
                """
            ),
            building, known, entrances
        ).update();
        this.bound(
            String.join(
                "",
                PostgisPwrEntrances.NEAREST,
                """
                INSERT INTO entrances (id, building_symbol, name, main, location, floor_id,
                    marking, marking_date, marking_source, approach, door_width_cm, pwr_name,
                    pwr_marking, pwr_marking_date)
                SELECT DISTINCT ON (id) id, :building, name, FALSE, spot, floor, marking,
                    COALESCE(CAST(:edited AS DATE), CURRENT_DATE), 'IMPORT', approach, width,
                    name, marking, COALESCE(CAST(:edited AS DATE), CURRENT_DATE)
                FROM (
                    SELECT 'pwr/' || ST_GeoHash(place.spot, 10) AS id, place.spot, pwr.floor,
                        pwr.marking, pwr.name, pwr.approach, pwr.width
                    FROM
                """,
                PostgisPwrEntrances.RECORDS,
                """
                    WHERE NOT pwr.exit AND pwr.floor = ANY(:known)
                        AND pwr.ord NOT IN (SELECT near.ord FROM near)
                ) AS record
                ORDER BY id
                ON CONFLICT (id, building_symbol) DO UPDATE SET name = EXCLUDED.name,
                    location = EXCLUDED.location, floor_id = EXCLUDED.floor_id,
                    approach = EXCLUDED.approach, door_width_cm = EXCLUDED.door_width_cm,
                    pwr_name = EXCLUDED.pwr_name, pwr_marking = EXCLUDED.pwr_marking,
                    pwr_marking_date = EXCLUDED.pwr_marking_date
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
        return this.jdbc.sql(sql).param("building", building)
            .param("known", known.toArray(String[]::new))
            .param("floors", entrances.stream().map(PlanEntrance::floorId).toArray(String[]::new))
            .param("points", entrances.stream().map(PlanEntrance::point).toArray(String[]::new))
            .param("markings", entrances.stream().map(PlanEntrance::marking).toArray(String[]::new))
            .param("names", entrances.stream().map(PlanEntrance::name).toArray(String[]::new))
            .param("approaches", PostgisPwrEntrances.approaches(entrances))
            .param("widths", PostgisPwrEntrances.widths(entrances))
            .param("exits", PostgisPwrEntrances.flags(entrances))
            .param("edited", this.edited);
    }

    private static String[] approaches(final List<PlanEntrance> entrances) {
        return entrances.stream().map(PlanEntrance::approach).toArray(String[]::new);
    }

    private static Integer[] widths(final List<PlanEntrance> entrances) {
        return entrances.stream().map(PlanEntrance::doorWidthCm).toArray(Integer[]::new);
    }

    private static Boolean[] flags(final List<PlanEntrance> entrances) {
        return entrances.stream().map(PlanEntrance::evacuationExit).toArray(Boolean[]::new);
    }
}
