/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.domain.FloorList;
import io.github.dostepnykampus.building.room.FloorRooms;
import io.github.dostepnykampus.building.room.RoomList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Pomieszczenia piętra z tabel floors i rooms, z obrysem piętra policzonym przy imporcie.
 * Winda podaje piętra pomieszczeń z tym samym lift_id od najniższego.
 *
 * <p>TODO(#2187): import planów zapisuje lift_id po połączeniu pomieszczeń windy w szyb.</p>
 *
 * <p>Wymagania: US16.1, NFR-40</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisFloorRooms implements FloorRooms {

    /**
     * Dostęp do tabel floors i rooms.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy odczyt pomieszczeń na bazie serwera.
     *
     * @param jdbc Dostęp do tabel floors i rooms
     */
    public PostgisFloorRooms(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<RoomList> rooms(final String building, final String floor) {
        return this.jdbc.sql(
            """
            SELECT COALESCE(
                ST_AsGeoJSON(outline), '{"type":"MultiPolygon","coordinates":[]}'
            ) FROM floors WHERE building_symbol = :building AND id = :floor
            """
        ).param("building", building).param("floor", floor).query(String.class).optional().map(
            outline -> new RoomList(
                FloorList.PWR_SOURCE, outline, this.listed(building, floor)
            )
        );
    }

    private List<RoomList.Room> listed(final String building, final String floor) {
        return this.jdbc.sql(
            """
            SELECT id, number, function, category, ST_AsGeoJSON(outline) AS shape,
                ST_AsGeoJSON(label_point) AS label, labelled, lift_id,
                ARRAY(
                    SELECT lift.floor_id
                    FROM rooms lift JOIN floors level
                        ON level.building_symbol = lift.building_symbol AND level.id = lift.floor_id
                    WHERE lift.building_symbol = room.building_symbol
                        AND lift.lift_id = room.lift_id
                    GROUP BY lift.floor_id, level.sort_order
                    ORDER BY level.sort_order
                ) AS lift_floors
            FROM rooms room WHERE building_symbol = :building AND floor_id = :floor
            ORDER BY id
            """
        ).param("building", building).param("floor", floor).query(
            (rows, num) -> {
                final List<String> floors = List.of(
                    (String[]) rows.getArray("lift_floors").getArray()
                );
                return new RoomList.Room(
                    rows.getString("id"), rows.getString("number"), rows.getString("function"),
                    rows.getString("category"), rows.getString("shape"),
                    rows.getString("label"), rows.getBoolean("labelled"),
                    Optional.ofNullable(rows.getString("lift_id"))
                        .map(lift -> new RoomList.Lift(lift, floors)).orElse(null)
                );
            }
        ).list();
    }
}
