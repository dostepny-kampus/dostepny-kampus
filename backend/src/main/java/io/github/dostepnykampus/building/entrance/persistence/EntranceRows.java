/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.entrance.persistence;

import io.github.dostepnykampus.building.domain.Door;
import io.github.dostepnykampus.building.domain.Entrance;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.RowMapper;

/**
 * Wiersz wejścia z regułą nazwy i oznaczenia oraz szerokość drzwi wejścia albo wyjścia. Nazwa
 * wejścia z planów PWr wygrywa z OpenStreetMap; wejście bez nazwy w danych ma nazwę „Wejście”
 * albo „Wejście główne” i opis położenia, które nadaje import. Oznaczenie z datą: obchód, potem
 * plany PWr, potem OpenStreetMap.
 *
 * <p>Wymagania: US2, US2.1, BR-24, BR-25, BR-27, US17.3</p>
 *
 * @since 0.1.0
 */
public final class EntranceRows implements RowMapper<Entrance> {

    /** Odczyt wejść bez warunku i kolejności; wywołujący dopisuje WHERE i ORDER BY. */
    public static final String SELECT = """
        SELECT building_symbol, id,
            COALESCE(entrances.pwr_name, entrances.name, place.place_name,
                CASE WHEN pwr_marking IS NOT NULL THEN 'Wejście' END) AS name,
            COALESCE(entrances.pwr_name, entrances.name) IS NULL
                AND (place.place_name IS NOT NULL OR pwr_marking IS NOT NULL) AS generated_name,
            place.place_description,
            type, main,
            ST_AsGeoJSON(location) AS location, floor_id,
            CASE WHEN marking_source = 'WALKTHROUGH' THEN marking
                ELSE COALESCE(pwr_marking, marking) END AS marking,
            CASE WHEN marking_source = 'WALKTHROUGH' THEN marking_date
                ELSE COALESCE(pwr_marking_date, marking_date) END::text AS marking_date,
            marking_source,
            pwr_marking IS NOT NULL AS pwr, approach, door_width_cm
        FROM entrances LEFT JOIN entrance_places AS place USING (id, building_symbol)
        """;

    @Override
    public Entrance mapRow(final ResultSet rows, final int num) throws SQLException {
        return new Entrance(
            EntranceRows.text(rows, "id"),
            new Entrance.Name(
                rows.getString("name"), rows.getBoolean("generated_name"),
                EntranceRows.approach(rows.getString("place_description"))
            ),
            rows.getString("type"),
            rows.getBoolean("main"),
            EntranceRows.text(rows, "location"),
            rows.getString("floor_id"),
            new Entrance.Marking(
                EntranceRows.text(rows, "marking"),
                EntranceRows.text(rows, "marking_date"),
                EntranceRows.text(rows, "marking_source")
            ),
            EntranceRows.layer(rows)
        );
    }

    private static Entrance.Layer layer(final ResultSet rows) throws SQLException {
        final Entrance.Layer result;
        if (rows.getBoolean("pwr")) {
            result = new Entrance.Layer.Listed(
                EntranceRows.approach(rows.getString("approach")), EntranceRows.door(rows)
            );
        } else {
            result = new Entrance.Layer.Unlisted();
        }
        return result;
    }

    private static Entrance.Approach approach(final @Nullable String text) {
        final Entrance.Approach result;
        if (text == null) {
            result = new Entrance.Approach.Undescribed();
        } else {
            result = new Entrance.Approach.Described(text);
        }
        return result;
    }

    /**
     * Szerokość drzwi z kolumny {@code door_width_cm}; brak albo NULL to drzwi niezmierzone.
     *
     * @param rows Wiersz wyniku
     * @return Drzwi zmierzone albo niezmierzone
     * @throws SQLException Gdy odczyt kolumny się nie uda
     */
    public static Door door(final ResultSet rows) throws SQLException {
        final Integer width = rows.getObject("door_width_cm", Integer.class);
        final Door result;
        if (width == null) {
            result = new Door.Unmeasured();
        } else {
            result = new Door.Measured(width);
        }
        return result;
    }

    private static String text(final ResultSet rows, final String column)
        throws SQLException {
        final String value = rows.getString(column);
        if (value == null) {
            throw new SQLException("Brak wartości kolumny %s".formatted(column));
        }
        return value;
    }
}
