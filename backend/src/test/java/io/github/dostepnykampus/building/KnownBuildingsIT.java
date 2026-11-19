/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.SampleBuilding;
import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Budynek zgłoszenia podany przez klienta jest znany tylko wtedy, gdy istnieje w bazie.
 * Wejście daje punkt, budynek i piętro; wejście wspólne bierze budynek z podpowiedzi.
 *
 * <p>Wymagania: BR-8, BR-14</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class KnownBuildingsIT {

    @Test
    void knowsOnlyExistingBuilding(final ApplicationContext context) {
        final KnownBuildings buildings = context.getBean(KnownBuildings.class);
        Assertions.assertEquals(
            List.of(true, false),
            List.of(buildings.known("A-1"), buildings.known("J-NONE")),
            "Budynek A-1 nieznany albo nieistniejący budynek uznany za znany"
        );
    }

    @Test
    void findsPlaceOfEntranceInHintedBuilding(final ApplicationContext context) {
        final JdbcClient jdbc = context.getBean(JdbcClient.class);
        final String outline = "POLYGON((17 51,17.001 51,17.001 51.001,17 51.001,17 51))";
        new SampleBuilding("G-931", "Pierwszy budynek", "BASIC").wkt(jdbc, outline);
        new SampleBuilding("G-932", "Drugi budynek", "BASIC").wkt(jdbc, outline);
        jdbc.sql(
            """
            INSERT INTO entrances (id, building_symbol, main, location, floor_id, marking,
                marking_date, marking_source)
            SELECT 'node/93', symbol, FALSE, ST_SetSRID(ST_MakePoint(17.0005, 51.0), 4326),
                floor, 'NO_DATA', DATE '2026-11-01', 'IMPORT'
            FROM (VALUES ('G-931', NULL), ('G-932', 'P0')) AS entrance (symbol, floor)
            """
        ).update();
        final KnownBuildings buildings = context.getBean(KnownBuildings.class);
        Assertions.assertEquals(
            List.of(
                Optional.of(new EntrancePlace(51.0, 17.0005, "G-932", "P0")),
                Optional.of(new EntrancePlace(51.0, 17.0005, "G-931", null)),
                Optional.empty()
            ),
            List.of(
                buildings.entrance("node/93", "G-932"),
                buildings.entrance("node/93", null),
                buildings.entrance("node/0", null)
            ),
            "Wejście bez miejsca, z budynkiem spoza podpowiedzi albo nieznane wejście z miejscem"
        );
    }
}
