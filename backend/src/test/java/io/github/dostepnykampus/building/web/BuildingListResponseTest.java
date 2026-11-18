/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.Building;
import io.github.dostepnykampus.building.domain.BuildingList;
import io.github.dostepnykampus.building.domain.Entrance;
import java.util.List;
import java.util.Map;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

/**
 * Każdy budynek listy dostaje w odpowiedzi swoje wejścia, a budynek bez wejść pustą listę.
 *
 * <p>Wymagania: US1.4, BR-27</p>
 *
 * @since 0.1.0
 */
final class BuildingListResponseTest {

    @Test
    void givesEveryBuildingOwnEntrances() {
        MatcherAssert.assertThat(
            "Budynek listy nie dostał swoich wejść albo budynek bez wejść dostał cudze",
            new BuildingListResponse(
                new BuildingList(
                    "© OpenStreetMap contributors",
                    List.of(
                        new Building("A-1", "Budynek A-1", "DETAILED", "{}"),
                        new Building("A-2", null, "BASIC", "{}")
                    ),
                    Map.of(
                        "A-1",
                        List.of(
                            new Entrance(
                                "pwr/s00000001", "Wejście", true, null, false,
                                "{\"type\":\"Point\",\"coordinates\":[17.0,51.0]}", "P0",
                                new Entrance.Marking("ACCESSIBLE", "2026-11-01", "IMPORT"),
                                new Entrance.Layer.Unlisted()
                            )
                        )
                    )
                )
            ).buildings().stream()
                .map(building -> building.entrances().stream().map(EntranceResponse::id).toList())
                .toList(),
            Matchers.contains(List.of("pwr/s00000001"), List.<String>of())
        );
    }
}
