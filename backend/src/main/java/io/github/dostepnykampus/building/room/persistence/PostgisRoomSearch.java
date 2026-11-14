/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.room.persistence;

import io.github.dostepnykampus.building.domain.Floor;
import io.github.dostepnykampus.building.room.RoomHit;
import io.github.dostepnykampus.building.room.RoomList;
import io.github.dostepnykampus.building.room.RoomSearch;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Wyszukiwanie sali w tabelach rooms i floors. Z kilku pomieszczeń o tym samym numerze na piętrze
 * wynikiem jest pomieszczenie z podpisem numeru, a bez niego największe.
 *
 * <p>Wymagania: US18.1</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisRoomSearch implements RoomSearch {

    /**
     * Dostęp do tabel rooms i floors.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy wyszukiwanie sali na bazie serwera.
     *
     * @param jdbc Dostęp do tabel rooms i floors
     */
    public PostgisRoomSearch(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<RoomHit> rooms(final String number) {
        return this.found(number, null);
    }

    @Override
    public List<RoomHit> rooms(final String number, final String building) {
        return this.found(number, building);
    }

    private List<RoomHit> found(final String number, final @Nullable String building) {
        return this.jdbc.sql(
            """
            SELECT * FROM (
                SELECT DISTINCT ON (r.building_symbol, r.floor_id, UPPER(r.number))
                    r.building_symbol, r.floor_id, f.label, f.sort_order, r.id, r.number,
                    r.function, r.category, ST_AsGeoJSON(r.outline) AS shape,
                    ST_AsGeoJSON(r.label_point) AS point, r.labelled,
                    UPPER(r.number) = UPPER(:number) AS exact
                FROM rooms r JOIN floors f
                    ON f.building_symbol = r.building_symbol AND f.id = r.floor_id
                WHERE UPPER(r.number) LIKE UPPER(:prefix) ESCAPE '!'
                    AND (CAST(:building AS TEXT) IS NULL OR r.building_symbol = :building)
                ORDER BY r.building_symbol, r.floor_id, UPPER(r.number), r.labelled DESC,
                    ST_Area(r.outline::geography) DESC, r.id
            ) AS hit
            ORDER BY exact DESC, building_symbol, sort_order, number
            LIMIT 50
            """
        ).param("number", number)
            .param("prefix", number.replaceAll("[!%_]", "!$0").concat("%"))
            .param("building", building).query(
                (rows, num) -> new RoomHit(
                    rows.getString("building_symbol"),
                    new Floor(
                        rows.getString("floor_id"), rows.getString("label"),
                        rows.getInt("sort_order")
                    ),
                    new RoomList.Room(
                        rows.getString("id"), rows.getString("number"),
                        rows.getString("function"), rows.getString("category"),
                        rows.getString("shape"), rows.getString("point"),
                        rows.getBoolean("labelled")
                    )
                )
            ).list();
    }
}
