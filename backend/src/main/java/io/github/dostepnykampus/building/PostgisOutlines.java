/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Kontury budynków w PostGIS dla innych modułów (BR-14): tylko zawieranie
 * punktu w konturze, bez promienia podpowiedzi i bez odpowiedzi REST.
 *
 * @since 0.1.0
 */
@Component
public final class PostgisOutlines implements Outlines {

    /**
     * Dostęp do tabeli buildings.
     */
    private final JdbcClient jdbc;

    PostgisOutlines(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Optional<String> containing(final double lat, final double lon) {
        return this.jdbc.sql(
            """
            SELECT symbol
            FROM buildings
            WHERE ST_Contains(
                outline, ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)
            )
            ORDER BY symbol
            LIMIT 1
            """
        ).param("lat", lat)
            .param("lon", lon)
            .query(String.class)
            .optional();
    }
}
