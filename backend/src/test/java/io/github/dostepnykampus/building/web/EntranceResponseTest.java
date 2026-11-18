/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.building.web;

import io.github.dostepnykampus.building.domain.Door;
import io.github.dostepnykampus.building.domain.Entrance;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Obecność wejścia w warstwie planów PWr staje się polami odpowiedzi bez wartości
 * zastępczych w miejscu brakujących danych.
 *
 * <p>Wymagania: BR-27, US3.3, US17.3</p>
 *
 * @since 0.1.0
 */
final class EntranceResponseTest {

    @Test
    void givesApproachWidthAndSourceOfListedEntrance() {
        MatcherAssert.assertThat(
            "Wejście z warstwy nie ma opisu dojścia, szerokości drzwi albo źródła",
            EntranceResponse.Plan.of(
                new Entrance.Layer.Listed(
                    new Entrance.Approach.Described("Od ul. Wybrzeże."), new Door.Measured(1)
                )
            ),
            Matchers.equalTo(
                new EntranceResponse.Plan("Od ul. Wybrzeże.", 1, "Politechnika Wrocławska")
            )
        );
    }

    @Test
    void leavesMissingApproachAndWidthOut() {
        MatcherAssert.assertThat(
            "Brak opisu dojścia albo szerokości drzwi dał wartość zastępczą",
            EntranceResponse.Plan.of(
                new Entrance.Layer.Listed(
                    new Entrance.Approach.Undescribed(), new Door.Unmeasured()
                )
            ),
            Matchers.equalTo(new EntranceResponse.Plan(null, null, "Politechnika Wrocławska"))
        );
    }

    @Test
    void givesNoPlanDataOutsideLayer() {
        MatcherAssert.assertThat(
            "Wejście spoza warstwy dostało dane z planów PWr",
            EntranceResponse.Plan.of(new Entrance.Layer.Unlisted()),
            Matchers.nullValue()
        );
    }

    @Test
    void rejectsDoorWithoutWidth() {
        Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new Door.Measured(0),
            "Drzwi o szerokości 0 cm zostały przyjęte jako zmierzone"
        );
    }
}
