/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.api.CampusArea;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Teren PWr w PostGIS: punkt należy do terenu, gdy któraś z granic
 * w tabeli campus_areas go pokrywa, także na linii granicy.
 *
 * <p>Wymagania: BR-G5</p>
 *
 * @since 0.1.0
 */
@Component
public final class PostgisCampusArea implements CampusArea {

    /**
     * Dostęp do tabeli campus_areas.
     */
    private final JdbcClient jdbc;

    /**
     * Tworzy adapter.
     *
     * @param jdbc Dostęp do tabeli campus_areas
     */
    public PostgisCampusArea(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public boolean covers(final double lat, final double lon) {
        return this.jdbc.sql(
            """
            SELECT EXISTS (
                SELECT 1 FROM campus_areas
                WHERE ST_Covers(boundary, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326))
            )
            """
        ).param("lat", lat)
            .param("lon", lon)
            .query(Boolean.class)
            .single();
    }
}
