/*
 * Dostępny Kampus
 * Author: Yevhenii Lukianenko
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.inspection.persistence;

import io.github.dostepnykampus.IntegrationTest;
import io.github.dostepnykampus.building.inspection.InspectionMarkers;
import io.github.dostepnykampus.building.inspection.NewMarker;
import java.util.List;
import java.util.UUID;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

/**
 * Testy zapisu znaczników pięter obejrzanych.
 *
 * @since 0.1.0
 */
@IntegrationTest
@Transactional
final class JdbcInspectionMarkersIT {

    /**
     * Konto Zbierającego, który stawia znaczniki.
     */
    private static final String AUTHOR = "konto-zbierajacego";

    @Test
    void keepsFirstMarkerWhenSameIdentifierComesAgain(final ApplicationContext context) {
        final InspectionMarkers markers = context.getBean(InspectionMarkers.class);
        final UUID floor = UUID.randomUUID();
        final String building = "G-920";
        markers.set(new NewMarker(floor, building, "G-920-1"), JdbcInspectionMarkersIT.AUTHOR);
        markers.set(new NewMarker(floor, building, "G-920-2"), "inne-konto");
        markers.set(
            new NewMarker(UUID.randomUUID(), building, null), JdbcInspectionMarkersIT.AUTHOR
        );
        MatcherAssert.assertThat(
            "Powtórzony identyfikator nie zmienia znacznika, a brak piętra to cały budynek",
            JdbcInspectionMarkersIT.active(context, building),
            Matchers.containsInAnyOrder("G-920-1:konto-zbierajacego", "budynek:konto-zbierajacego")
        );
    }

    private static List<String> active(final ApplicationContext context, final String building) {
        return context.getBean(JdbcClient.class).sql(
            """
            SELECT COALESCE(floor_id, 'budynek') || ':' || author FROM inspection_markers
            WHERE building_symbol = ? AND revoked_at IS NULL
            """
        ).param(building).query(String.class).list();
    }
}
