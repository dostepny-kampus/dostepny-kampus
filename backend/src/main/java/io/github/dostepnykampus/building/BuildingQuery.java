/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

/**
 * Odczyt budynków z tabeli buildings do listy na mapie (US1) i ich wejść
 * z tabeli entrances na kartę budynku (US2).
 *
 * @since 0.1.0
 */
@Service
final class BuildingQuery {

    /**
     * Promień podpowiedzi budynku w metrach, większy niż błąd GNSS 15 m (NFR-3).
     */
    static final int NEAR_METERS = 50;

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    /**
     * Parser GeoJSON zwracanego przez ST_AsGeoJSON.
     */
    private final ObjectMapper mapper;

    BuildingQuery(final JdbcClient jdbc, final ObjectMapper mapper) {
        this.jdbc = jdbc;
        this.mapper = mapper;
    }

    BuildingList listBuildings() {
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level, source,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            ORDER BY symbol
            """
        ).query(this::rows);
    }

    LocateResult locate(final double lat, final double lon) {
        return this.jdbc.sql(
            """
            WITH point AS (
                SELECT ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geom
            )
            SELECT buildings.symbol AS symbol,
                ST_Contains(buildings.outline, point.geom) AS inside
            FROM buildings CROSS JOIN point
            WHERE ST_Contains(buildings.outline, point.geom)
                OR ST_DWithin(buildings.outline::geography,
                    point.geom::geography, :meters)
            ORDER BY inside DESC,
                ST_Distance(buildings.outline::geography,
                    point.geom::geography),
                buildings.symbol
            LIMIT 1
            """
        ).param("lat", lat)
            .param("lon", lon)
            .param("meters", BuildingQuery.NEAR_METERS)
            .query(BuildingQuery::hit);
    }

    Optional<BuildingCard> building(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            WHERE symbol = :symbol
            """
        ).param("symbol", symbol).query(this::card);
    }

    private List<Entrance> entrances(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT id, building_symbol, name, type, main,
                ST_AsGeoJSON(location) AS location, marking,
                marking_date::text AS marking_date, marking_source
            FROM entrances
            WHERE building_symbol = :symbol
            ORDER BY main DESC, id
            """
        ).param("symbol", symbol).query(
            (rows, num) -> new Entrance(
                BuildingQuery.text(rows, "id"),
                BuildingQuery.text(rows, "building_symbol"),
                rows.getString("name"),
                rows.getString("type"),
                rows.getBoolean("main"),
                this.mapper.readTree(BuildingQuery.text(rows, "location")),
                new Entrance.Marking(
                    BuildingQuery.text(rows, "marking"),
                    BuildingQuery.text(rows, "marking_date"),
                    BuildingQuery.text(rows, "marking_source")
                )
            )
        ).list();
    }

    private BuildingList rows(final ResultSet rows) throws SQLException {
        final List<Building> found = new ArrayList<>(8);
        String chosen = "© OpenStreetMap contributors";
        while (rows.next()) {
            if (found.isEmpty()) {
                chosen = BuildingQuery.text(rows, "source");
            }
            found.add(this.one(rows));
        }
        return new BuildingList(chosen, found);
    }

    private Building one(final ResultSet rows) throws SQLException {
        return new Building(
            BuildingQuery.text(rows, "symbol"),
            BuildingQuery.text(rows, "name"),
            BuildingQuery.text(rows, "detail_level"),
            this.mapper.readTree(BuildingQuery.text(rows, "outline"))
        );
    }

    private Optional<BuildingCard> card(final ResultSet rows) throws SQLException {
        final Optional<BuildingCard> found;
        if (rows.next()) {
            final Building item = this.one(rows);
            found = Optional.of(
                new BuildingCard(
                    item.symbol(),
                    item.name(),
                    item.detailLevel(),
                    item.outline(),
                    this.entrances(item.symbol())
                )
            );
        } else {
            found = Optional.empty();
        }
        return found;
    }

    private static LocateResult hit(final ResultSet rows) throws SQLException {
        final LocateResult found;
        if (rows.next()) {
            found = new LocateResult(
                BuildingQuery.text(rows, "symbol"),
                rows.getBoolean("inside")
            );
        } else {
            found = new LocateResult(null, false);
        }
        return found;
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
