/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.postgis;

import io.github.dostepnykampus.building.CampusArea;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Teren PWr w PostGIS (BR-G5): odległość punktu od konturów budynków liczy
 * baza na elipsoidzie, w metrach na ziemi.
 *
 * @since 0.1.0
 */
@Component
public final class PostgisCampusArea implements CampusArea {

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli buildings
     */
    public PostgisCampusArea(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean covers(final double lat, final double lon) {
        return this.jdbc.sql(
            """
            SELECT EXISTS (
                SELECT 1 FROM buildings
                WHERE ST_DWithin(
                    outline::geography,
                    ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                    :radius
                )
            )
            """
        ).param("lat", lat)
            .param("lon", lon)
            .param("radius", CampusArea.RADIUS_METERS)
            .query(Boolean.class)
            .single();
    }
}
