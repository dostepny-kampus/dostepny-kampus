/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;

/**
 * Budynek zgłoszenia podany przez klienta jest znany tylko wtedy, gdy istnieje w bazie.
 *
 * <p>Wymagania: BR-14</p>
 *
 * @since 0.1.0
 */
@IntegrationTest
final class KnownBuildingsTest {

    @Test
    void knowsOnlyExistingBuilding(final ApplicationContext context) {
        final KnownBuildings buildings = context.getBean(KnownBuildings.class);
        Assertions.assertEquals(
            List.of(true, false),
            List.of(buildings.known("A-1"), buildings.known("J-NONE")),
            "Budynek A-1 nieznany albo nieistniejący budynek uznany za znany"
        );
    }
}
