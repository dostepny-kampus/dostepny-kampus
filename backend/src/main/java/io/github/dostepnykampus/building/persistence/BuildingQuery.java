/*
 * Dostępny Kampus
 * Author: Myroslav Hryniuka, Vladyslav Yevkov, Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.api.Outlines;
import io.github.dostepnykampus.building.domain.Building;
import io.github.dostepnykampus.building.domain.BuildingCard;
import io.github.dostepnykampus.building.domain.BuildingList;
import io.github.dostepnykampus.building.domain.Buildings;
import io.github.dostepnykampus.building.domain.Door;
import io.github.dostepnykampus.building.domain.Entrance;
import io.github.dostepnykampus.building.domain.EvacuationExit;
import io.github.dostepnykampus.building.domain.Floor;
import io.github.dostepnykampus.building.domain.FloorList;
import io.github.dostepnykampus.building.domain.Placement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

/**
 * Odczyt REST budynków: lista na mapie i karta budynku z wejściami, karta też z wyjściami
 * ewakuacyjnymi, oraz podpowiedź budynku dla punktu. Nazwa wejścia z planów PWr wygrywa
 * z OpenStreetMap; oznaczenie z datą: obchód, potem plany PWr, potem OpenStreetMap. Zawieranie
 * punktu liczy tylko {@link Outlines}, więc podpowiedź i budynek zgłoszenia szybkiego są zawsze
 * równe. Atrybucja listy to źródła konturów bez powtórzeń, alfabetycznie, rozdzielone średnikiem;
 * pusta tabela ma pustą atrybucję.
 *
 * <p>Wymagania: US1, BR-G2, US2, BR-14, NFR-40, US16, BR-24, BR-27, US17.3, US2.6, US1.4</p>
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
     * Odczyt wejść z regułą nazwy i oznaczenia, bez warunku i kolejności.
     */
    private static final String ENTRANCES = """
        SELECT building_symbol, id, COALESCE(pwr_name, name) AS name, type, main,
            ST_AsGeoJSON(location) AS location, floor_id,
            CASE WHEN marking_source = 'WALKTHROUGH' THEN marking
                ELSE COALESCE(pwr_marking, marking) END AS marking,
            CASE WHEN marking_source = 'WALKTHROUGH' THEN marking_date
                ELSE COALESCE(pwr_marking_date, marking_date) END::text AS marking_date,
            marking_source,
            pwr_marking IS NOT NULL AS pwr, approach, door_width_cm
        FROM entrances
        """;

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
        final Map<String, List<Entrance>> entrances = this.jdbc.sql(
            BuildingQuery.ENTRANCES.concat("ORDER BY building_symbol, main DESC, id")
        ).query(
            (rows, num) -> Map.entry(
                BuildingQuery.text(rows, "building_symbol"), BuildingQuery.entrance(rows)
            )
        ).list().stream().collect(
            Collectors.groupingBy(
                Map.Entry::getKey,
                Collectors.mapping(Map.Entry::getValue, Collectors.toUnmodifiableList())
            )
        );
        return this.jdbc.sql(
            """
            SELECT symbol, name, detail_level, source,
                ST_AsGeoJSON(outline) AS outline
            FROM buildings
            ORDER BY symbol
            """
        ).query((final ResultSet rows) -> BuildingQuery.rows(rows, entrances));
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

    @Override
    public Optional<FloorList> floors(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT symbol
            FROM buildings
            WHERE symbol = ?
            """
        ).param(symbol).query(String.class).optional().map(
            found -> new FloorList(FloorList.PWR_SOURCE, this.buildingFloors(found))
        );
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
            this.entrances(item.symbol()),
            this.exits(item.symbol())
        );
    }

    private List<Entrance> entrances(final String symbol) {
        return this.jdbc.sql(
            BuildingQuery.ENTRANCES.concat(
                "WHERE building_symbol = :symbol ORDER BY main DESC, id"
            )
        ).param("symbol", symbol).query(
            (rows, num) -> BuildingQuery.entrance(rows)
        ).list();
    }

    private List<EvacuationExit> exits(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT id, ST_AsGeoJSON(location) AS location, floor_id, description, door_width_cm
            FROM evacuation_exits
            WHERE building_symbol = :symbol
            ORDER BY id
            """
        ).param("symbol", symbol).query(
            (rows, num) -> new EvacuationExit(
                BuildingQuery.text(rows, "id"),
                BuildingQuery.text(rows, "location"),
                BuildingQuery.text(rows, "floor_id"),
                BuildingQuery.text(rows, "description"),
                BuildingQuery.door(rows)
            )
        ).list();
    }

    private List<Floor> buildingFloors(final String symbol) {
        return this.jdbc.sql(
            """
            SELECT id, label, sort_order
            FROM floors
            WHERE building_symbol = ?
            ORDER BY sort_order, id
            """
        ).param(symbol).query(
            (rows, num) -> new Floor(
                BuildingQuery.text(rows, "id"),
                BuildingQuery.text(rows, "label"),
                rows.getInt("sort_order")
            )
        ).list();
    }

    private static BuildingList rows(
        final ResultSet rows, final Map<String, List<Entrance>> entrances
    ) throws SQLException {
        final List<Building> found = new ArrayList<>(8);
        final SortedSet<String> sources = new TreeSet<>();
        while (rows.next()) {
            sources.add(BuildingQuery.text(rows, "source"));
            found.add(BuildingQuery.one(rows));
        }
        return new BuildingList(String.join("; ", sources), found, entrances);
    }

    private static Entrance entrance(final ResultSet rows) throws SQLException {
        return new Entrance(
            BuildingQuery.text(rows, "id"),
            rows.getString("name"),
            rows.getString("type"),
            rows.getBoolean("main"),
            BuildingQuery.text(rows, "location"),
            rows.getString("floor_id"),
            new Entrance.Marking(
                BuildingQuery.text(rows, "marking"),
                BuildingQuery.text(rows, "marking_date"),
                BuildingQuery.text(rows, "marking_source")
            ),
            BuildingQuery.layer(rows)
        );
    }

    private static Entrance.Layer layer(final ResultSet rows) throws SQLException {
        final Entrance.Layer result;
        if (rows.getBoolean("pwr")) {
            result = new Entrance.Layer.Listed(
                BuildingQuery.approach(rows.getString("approach")), BuildingQuery.door(rows)
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

    private static Door door(final ResultSet rows) throws SQLException {
        final Integer width = rows.getObject("door_width_cm", Integer.class);
        final Door result;
        if (width == null) {
            result = new Door.Unmeasured();
        } else {
            result = new Door.Measured(width);
        }
        return result;
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
