/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.Outlines;
import io.github.dostepnykampus.building.domain.Building;
import io.github.dostepnykampus.building.domain.BuildingCard;
import io.github.dostepnykampus.building.domain.BuildingList;
import io.github.dostepnykampus.building.domain.Buildings;
import io.github.dostepnykampus.building.domain.Entrance;
import io.github.dostepnykampus.building.domain.Placement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Odczyt REST budynków: lista na mapie, podpowiedź budynku dla punktu i karta budynku
 * z wejściami. Zawieranie punktu liczy tylko {@link Outlines}, więc podpowiedź i budynek
 * zapisany przy zgłoszeniu szybkim są zawsze równe. Atrybucja listy to źródła konturów
 * bez powtórzeń, alfabetycznie, rozdzielone średnikiem; pusta tabela ma pustą atrybucję.
 *
 * <p>Wymagania: US1, BR-G2, US2, BR-14, NFR-40</p>
 *
 * @since 0.1.0
 */
@Service
public final class BuildingQuery implements Buildings {

    /**
     * Promień podpowiedzi budynku w metrach, większy niż błąd GNSS 15 m.
     *
     * <p>Wymagania: NFR-3</p>
     */
    static final int NEAR_METERS = 50;

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    /**
     * Budynek, w którego konturze leży punkt.
     */
    private final Outlines outlines;

    BuildingQuery(final JdbcClient jdbc, final Outlines outlines) {
        this.jdbc = jdbc;
        this.outlines = outlines;
    }

    @Override
    public BuildingList listBuildings() {
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level, source,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            ORDER BY symbol
            """
        ).query(BuildingQuery::rows);
    }

    @Override
    public Placement locate(final double lat, final double lon) {
        return this.outlines.containing(lat, lon)
            .<Placement>map(Placement.Inside::new)
            .orElseGet(() -> this.near(lat, lon));
    }

    @Override
    public Optional<BuildingCard> building(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            WHERE symbol = :symbol
            """
        ).param("symbol", symbol)
            .query((rows, num) -> BuildingQuery.one(rows))
            .optional()
            .map(this::card);
    }

    private Placement near(final double lat, final double lon) {
        return this.jdbc.sql(
            """
            WITH point AS (
                SELECT ST_SetSRID(ST_MakePoint(:lon, :lat), 4326) AS geom
            )
            SELECT buildings.symbol
            FROM buildings CROSS JOIN point
            WHERE ST_DWithin(buildings.outline::geography,
                point.geom::geography, :meters)
            ORDER BY ST_Distance(buildings.outline::geography,
                point.geom::geography),
                buildings.symbol
            LIMIT 1
            """
        ).param("lat", lat)
            .param("lon", lon)
            .param("meters", BuildingQuery.NEAR_METERS)
            .query(String.class)
            .optional()
            .<Placement>map(Placement.Near::new)
            .orElseGet(Placement.None::new);
    }

    private BuildingCard card(final Building item) {
        return new BuildingCard(
            item.symbol(),
            item.name(),
            item.detailLevel(),
            item.outline(),
            this.entrances(item.symbol())
        );
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
                BuildingQuery.text(rows, "location"),
                new Entrance.Marking(
                    BuildingQuery.text(rows, "marking"),
                    BuildingQuery.text(rows, "marking_date"),
                    BuildingQuery.text(rows, "marking_source")
                )
            )
        ).list();
    }

    private static BuildingList rows(final ResultSet rows) throws SQLException {
        final List<Building> found = new ArrayList<>(8);
        final SortedSet<String> sources = new TreeSet<>();
        while (rows.next()) {
            sources.add(BuildingQuery.text(rows, "source"));
            found.add(BuildingQuery.one(rows));
        }
        return new BuildingList(String.join("; ", sources), found);
    }

    private static Building one(final ResultSet rows) throws SQLException {
        return new Building(
            BuildingQuery.text(rows, "symbol"),
            rows.getString("name"),
            BuildingQuery.text(rows, "detail_level"),
            BuildingQuery.text(rows, "outline")
        );
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
