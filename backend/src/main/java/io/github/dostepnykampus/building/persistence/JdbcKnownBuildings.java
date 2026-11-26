/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Budynki znane serwerowi według tabeli buildings.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcKnownBuildings implements KnownBuildings {

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli buildings
     */
    public JdbcKnownBuildings(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean known(final String symbol) {
        return this.jdbc.sql("SELECT EXISTS (SELECT 1 FROM buildings WHERE symbol = :symbol)")
            .param("symbol", symbol)
            .query(Boolean.class)
            .single();
    }

    @Override
    public Optional<String> floorLabel(final @Nullable String symbol, final @Nullable String id) {
        return this.jdbc.sql(
            "SELECT label FROM floors WHERE building_symbol = :symbol AND id = :id"
        ).param("symbol", symbol).param("id", id).query(String.class).optional();
    }

    @Override
    public Optional<EntrancePlace> entrance(final String id, final @Nullable String symbol) {
        return this.jdbc.sql(
            String.join(
                " ",
                "SELECT ST_Y(location) AS lat, ST_X(location) AS lon, building_symbol, floor_id",
                "FROM entrances WHERE id = :id",
                "ORDER BY COALESCE(building_symbol = CAST(:symbol AS VARCHAR), FALSE) DESC,",
                "building_symbol",
                "LIMIT 1"
            )
        ).param("id", id).param("symbol", symbol).query(
            (row, num) -> new EntrancePlace(
                row.getDouble("lat"), row.getDouble("lon"),
                row.getString("building_symbol"), row.getString("floor_id")
            )
        ).optional();
    }
}
