/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.persistence;

import io.github.dostepnykampus.building.api.BuildingInventory;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

/**
 * Odczyt pięter i liczników wejść z katalogu budynków.
 *
 * <p>Wymagania: US11.1, US11.4, BR-24</p>
 *
 * @since 0.1.0
 */
@Component
public final class JdbcBuildingInventory implements BuildingInventory {

    /** Dostęp do danych katalogu. */
    private final JdbcClient jdbc;

    JdbcBuildingInventory(final JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<Building> buildings() {
        return this.jdbc.sql(
            """
            SELECT b.symbol, b.detail_level = 'DETAILED' AS detailed,
                ARRAY(SELECT f.id FROM floors f WHERE f.building_symbol = b.symbol) AS floors,
                (SELECT COUNT(*) FROM entrances e WHERE e.building_symbol = b.symbol) AS entrances,
                (SELECT COUNT(*) FROM entrances e WHERE e.building_symbol = b.symbol
                    AND e.marking_source = 'WALKTHROUGH') AS checked
            FROM buildings b
            ORDER BY b.symbol
            """
        ).query(
            (rows, num) -> new Building(
                rows.getString("symbol"), rows.getBoolean("detailed"),
                Set.of((String[]) rows.getArray("floors").getArray()),
                rows.getInt("entrances"), rows.getInt("checked")
            )
        ).list();
    }
}
