/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.BuildingPlan;
import io.github.dostepnykampus.building.room.FloorPlans;
import io.github.dostepnykampus.building.room.PlanEntrance;
import io.github.dostepnykampus.building.room.PlanFloor;
import io.github.dostepnykampus.building.room.PlanRoom;
import io.github.dostepnykampus.building.room.PwrFunction;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionOperations;

/**
 * Plan budynku w tabelach floors i rooms, zastępowany w jednej transakcji; piętro spoza planu
 * znika z pomieszczeniami, pusty numer to NULL, punkt podpisu leży w konturze. Podpis ma tylko
 * największe od 20 m² z pomieszczeń piętra o tym samym numerze. Obrys piętra to suma pomieszczeń
 * poszerzonych i zwężonych o 0,5 m. Budynek z planem jest szczegółowy (DETAILED). Wejście bierze
 * piętro najbliższego wejścia PWr z piętrem planu do 15 m. Windy nad sobą łączy w szyb (lift_id).
 *
 * <p>Wymagania: US16, US18, US18.2, BR-G1, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisPlans implements FloorPlans {

    /**
     * Dostęp do tabel floors i rooms.
     */
    private final JdbcClient jdbc;

    /**
     * Transakcja zapisu jednego budynku.
     */
    private final TransactionOperations transaction;

    /**
     * Tworzy zapis planów na bazie serwera.
     *
     * @param jdbc Dostęp do tabel floors i rooms
     * @param transaction Transakcja zapisu jednego budynku
     */
    public PostgisPlans(final JdbcClient jdbc, final TransactionOperations transaction) {
        this.jdbc = jdbc;
        this.transaction = transaction;
    }

    @Override
    public void save(final BuildingPlan plan) {
        final String building = plan.building();
        final List<PlanFloor> floors = plan.floors();
        final List<PlanRoom> rooms = plan.rooms();
        final List<PlanEntrance> entrances = plan.entrances();
        final Set<String> known = floors.stream().map(PlanFloor::id).collect(Collectors.toSet());
        this.transaction.executeWithoutResult(
            status -> {
                this.jdbc.sql("DELETE FROM rooms WHERE building_symbol = :building")
                    .param("building", building).update();
                floors.forEach(floor -> this.floor(building, floor));
                this.jdbc.sql(
                    """
                    DELETE FROM floors
                    WHERE building_symbol = :building AND NOT (id = ANY(:ids))
                    """
                ).param("building", building).param("ids", known.toArray(String[]::new))
                    .update();
                rooms.stream().filter(room -> known.contains(room.floorId()))
                    .forEach(room -> this.room(building, room));
                this.derive(building);
                this.jdbc.sql(
                    "UPDATE buildings SET detail_level = 'DETAILED' WHERE symbol = :building"
                ).param("building", building).update();
                this.entrances(building, known, entrances);
                new PostgisShafts(this.jdbc).link(building);
            }
        );
    }

    private void floor(final String building, final PlanFloor floor) {
        this.jdbc.sql(
            """
            INSERT INTO floors (building_symbol, id, label, sort_order)
            VALUES (:building, :id, :label, :order)
            ON CONFLICT (building_symbol, id)
            DO UPDATE SET label = EXCLUDED.label, sort_order = EXCLUDED.sort_order
            """
        ).param("building", building).param("id", floor.id())
            .param("label", floor.label()).param("order", floor.order()).update();
    }

    private void room(final String building, final PlanRoom room) {
        this.jdbc.sql(
            """
            INSERT INTO rooms (
                building_symbol, floor_id, id, number, function, category, outline, label_point
            )
            SELECT :building, :floor, :id, NULLIF(BTRIM(:number), ''), :function, :category, shape,
                ST_PointOnSurface(shape)
            FROM (SELECT ST_Multi(ST_SetSRID(ST_GeomFromGeoJSON(:outline), 4326)) AS shape) AS room
            """
        ).param("building", building).param("floor", room.floorId()).param("id", room.id())
            .param("number", room.number()).param("function", room.function())
            .param("category", new PwrFunction(room.function()).category().name())
            .param("outline", room.outline()).update();
    }

    private void derive(final String building) {
        this.jdbc.sql(
            """
            UPDATE rooms SET labelled = ranked.area >= 20 AND ranked.place = 1
            FROM (
                SELECT floor_id, id, ST_Area(outline::geography) AS area, row_number() OVER (
                    PARTITION BY floor_id, number
                    ORDER BY ST_Area(outline::geography) DESC, id
                ) AS place
                FROM rooms WHERE building_symbol = :building AND number IS NOT NULL
            ) AS ranked
            WHERE rooms.building_symbol = :building AND rooms.floor_id = ranked.floor_id
                AND rooms.id = ranked.id
            """
        ).param("building", building).update();
        this.jdbc.sql(
            """
            UPDATE floors SET outline = (
                SELECT ST_Multi(
                    ST_Buffer(
                        ST_Union(ST_Buffer(outline::geography, 0.5)::geometry)::geography, -0.5
                    )::geometry
                )
                FROM rooms
                WHERE rooms.building_symbol = floors.building_symbol AND rooms.floor_id = floors.id
            )
            WHERE building_symbol = :building
            """
        ).param("building", building).update();
    }

    private void entrances(
        final String building, final Set<String> known, final List<PlanEntrance> entrances
    ) {
        this.jdbc.sql(
            """
            UPDATE entrances SET floor_id = (
                SELECT pwr.floor
                FROM unnest(CAST(:floors AS TEXT[]), CAST(:points AS TEXT[])) AS pwr (floor, point),
                    LATERAL (
                        SELECT ST_SetSRID(ST_GeomFromGeoJSON(pwr.point), 4326)::geography AS spot
                    ) AS place
                WHERE pwr.floor = ANY(:known)
                    AND ST_DWithin(place.spot, entrances.location::geography, 15)
                ORDER BY ST_Distance(place.spot, entrances.location::geography), pwr.floor
                LIMIT 1
            )
            WHERE building_symbol = :building
            """
        ).param("building", building).param("known", known.toArray(String[]::new))
            .param("floors", entrances.stream().map(PlanEntrance::floorId).toArray(String[]::new))
            .param("points", entrances.stream().map(PlanEntrance::point).toArray(String[]::new))
            .update();
    }
}
