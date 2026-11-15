/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Punkt na terenie PWr albo przy jego granicy, wybrany z geometrii w pliku danych
 * {@code data/areas/pwr-areas.geojson}.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @param lat Szerokość geograficzna w stopniach WGS84
 * @param lon Długość geograficzna w stopniach WGS84
 * @since 0.1.0
 */
public record CampusPoint(double lat, double lon) {

    /**
     * Punkt w środku przerwy 60 m między budynkami P-20 i P-23 terenu Wittigowo.
     */
    public static final CampusPoint GAP = new CampusPoint(51.101_318_6, 17.088_970_1);

    /**
     * Punkt na terenie Wittigowo 20 m za obszarem uczelni, ponad 30 m od każdego budynku.
     */
    public static final CampusPoint MARGIN = new CampusPoint(51.101_438, 17.088_110_8);

    /**
     * Wierzchołek granicy terenu Wittigowo, czyli punkt na linii granicy.
     *
     * @param jdbc Dostęp do bazy
     * @return Wierzchołek położony najbardziej na zachód
     */
    public static CampusPoint onBoundary(final JdbcClient jdbc) {
        return jdbc.sql(
            """
            SELECT ST_Y(v.geom), ST_X(v.geom)
            FROM (SELECT (ST_DumpPoints(boundary)).geom AS geom
                FROM campus_areas WHERE name = 'Wittigowo') AS v
            ORDER BY ST_X(v.geom), ST_Y(v.geom) LIMIT 1
            """
        ).query((rows, number) -> new CampusPoint(rows.getDouble(1), rows.getDouble(2)))
            .single();
    }

    /**
     * Punkt za granicą wszystkich terenów: na zachód od wierzchołka granicy położonego
     * najbardziej na zachód, w metrach na ziemi.
     *
     * @param jdbc Dostęp do bazy
     * @param meters Odległość od tego wierzchołka
     * @return Punkt oddalony od granicy o podaną liczbę metrów
     */
    public static CampusPoint beyondBoundary(final JdbcClient jdbc, final int meters) {
        return jdbc.sql(
            """
            SELECT ST_Y(p), ST_X(p) FROM (
                SELECT ST_Project(v::geography, :meters, radians(270))::geometry AS p FROM (
                    SELECT (ST_DumpPoints(boundary)).geom AS v FROM campus_areas
                ) AS vertices ORDER BY ST_X(v) LIMIT 1
            ) AS west
            """
        ).param("meters", meters)
            .query((rows, number) -> new CampusPoint(rows.getDouble(1), rows.getDouble(2)))
            .single();
    }

    /**
     * Mówi, czy żaden budynek nie leży bliżej niż podana odległość.
     *
     * @param jdbc Dostęp do bazy
     * @param meters Odległość od konturów budynków w metrach na ziemi
     * @return Prawda, gdy punkt jest dalej niż podana odległość od każdego konturu
     */
    public boolean awayFromBuildings(final JdbcClient jdbc, final int meters) {
        return jdbc.sql(
            """
            SELECT NOT EXISTS (
                SELECT 1 FROM buildings
                WHERE ST_DWithin(
                    outline::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :meters
                )
            )
            """
        ).param("lat", this.lat).param("lon", this.lon).param("meters", meters)
            .query(Boolean.class).single();
    }

    /**
     * Punkt jako współrzędne GeoJSON.
     *
     * @return Tablica {@code [długość, szerokość]}
     */
    public String json() {
        return String.format("[%s,%s]", this.lon, this.lat);
    }
}
