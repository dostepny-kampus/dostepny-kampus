/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Szyby wind budynku z pomieszczeń ELEVATOR: dwa pomieszczenia z różnych pięter należą do jednej
 * windy, gdy część wspólna ich konturów ma co najmniej połowę powierzchni mniejszego, a połączenie
 * jest przechodnie. Winda dostaje lift_id {@code <piętro>/<pomieszczenie>} pomieszczenia na
 * najniższym piętrze szybu, więc ponowny import daje ten sam identyfikator.
 *
 * <p>Wymagania: US18.2</p>
 *
 * @since 0.1.0
 */
final class PostgisShafts {

    /**
     * Dostęp do tabel planu.
     */
    private final JdbcClient jdbc;

    /**
     * Szyby wind w tabelach planu.
     *
     * @param jdbc Dostęp do tabel planu
     */
    PostgisShafts(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Zapisuje lift_id pomieszczeń wind budynku.
     *
     * @param building Symbol budynku
     */
    void link(final String building) {
        final List<String> lifts = this.jdbc.sql(
            """
            SELECT room.floor_id || '/' || room.id FROM rooms room
            JOIN floors level ON level.building_symbol = room.building_symbol
                AND level.id = room.floor_id
            WHERE room.building_symbol = :building AND room.category = 'ELEVATOR'
            ORDER BY level.sort_order, room.id
            """
        ).param("building", building)
            .query((rows, num) -> Objects.requireNonNull(rows.getString(1))).list();
        final Map<String, String> shaft = new HashMap<>(lifts.size());
        lifts.forEach(lift -> shaft.put(lift, lift));
        this.jdbc.sql(
            """
            SELECT one.floor_id || '/' || one.id AS lower, two.floor_id || '/' || two.id AS upper
            FROM rooms one JOIN rooms two ON two.building_symbol = one.building_symbol
                AND two.floor_id <> one.floor_id AND ST_Intersects(one.outline, two.outline)
            WHERE one.building_symbol = :building AND one.category = 'ELEVATOR'
                AND two.category = 'ELEVATOR'
                AND ST_Area(ST_Intersection(one.outline, two.outline)::geography)
                    >= 0.5 * LEAST(ST_Area(one.outline::geography), ST_Area(two.outline::geography))
            """
        ).param("building", building).query(
            (rows, num) -> List.of(rows.getString("lower"), rows.getString("upper"))
        ).list().forEach(
            pair -> {
                final String first = PostgisShafts.root(shaft, pair.get(0));
                final String second = PostgisShafts.root(shaft, pair.get(1));
                if (lifts.indexOf(first) < lifts.indexOf(second)) {
                    shaft.put(second, first);
                } else {
                    shaft.put(first, second);
                }
            }
        );
        this.jdbc.sql(
            """
            UPDATE rooms SET lift_id = lift.shaft
            FROM unnest(CAST(:rooms AS TEXT[]), CAST(:shafts AS TEXT[])) AS lift (room, shaft)
            WHERE rooms.building_symbol = :building
                AND rooms.floor_id || '/' || rooms.id = lift.room
            """
        ).param("building", building).param("rooms", lifts.toArray(String[]::new)).param(
            "shafts",
            lifts.stream().map(lift -> PostgisShafts.root(shaft, lift)).toArray(String[]::new)
        ).update();
    }

    private static String root(final Map<String, String> shaft, final String lift) {
        String found = lift;
        while (!Objects.equals(shaft.get(found), found)) {
            found = Objects.requireNonNull(shaft.get(found));
        }
        return found;
    }
}
