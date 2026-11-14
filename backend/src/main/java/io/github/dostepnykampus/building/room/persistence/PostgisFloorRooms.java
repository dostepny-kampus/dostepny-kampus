/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.room.FloorRooms;
import io.github.dostepnykampus.building.room.RoomList;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Pomieszczenia piętra z tabel floors i rooms, z obrysem piętra policzonym przy imporcie.
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
                "Politechnika Wrocławska", outline,
                this.jdbc.sql(
                    """
                    SELECT id, number, function, category, ST_AsGeoJSON(outline) AS shape,
                        ST_AsGeoJSON(label_point) AS label, labelled
                    FROM rooms WHERE building_symbol = :building AND floor_id = :floor
                    ORDER BY id
                    """
                ).param("building", building).param("floor", floor).query(
                    (rows, num) -> new RoomList.Room(
                        rows.getString("id"), rows.getString("number"),
                        rows.getString("function"), rows.getString("category"),
                        rows.getString("shape"), rows.getString("label"),
                        rows.getBoolean("labelled")
                    )
                ).list()
            )
        );
    }
}
