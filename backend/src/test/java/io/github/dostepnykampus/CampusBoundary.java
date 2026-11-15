/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus;

import org.springframework.jdbc.core.simple.JdbcClient;

/**
 * Punkty wyliczane z granic terenów PWr w tabeli {@code campus_areas}.
 *
 * <p>Wymagania: BR-G5, US4.9</p>
 *
 * @since 0.1.0
 */
public final class CampusBoundary {

    /**
     * Dostęp do tabeli campus_areas.
     */
    private final JdbcClient jdbc;

    /**
     * Granice w bazie.
     *
     * @param jdbc Dostęp do bazy
     */
    public CampusBoundary(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * Wierzchołek granicy terenu Wittigowo, czyli punkt na linii granicy.
     *
     * @return Wierzchołek położony najbardziej na zachód
     */
    public CampusPoint onLine() {
        return this.jdbc.sql(
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
     * @param meters Odległość od tego wierzchołka
     * @return Punkt oddalony od granicy o podaną liczbę metrów
     */
    public CampusPoint beyond(final int meters) {
        return this.jdbc.sql(
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
}
