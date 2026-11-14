/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.api.KnownBuildings;
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
}
