/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPlace;
import io.github.dostepnykampus.barrier.submission.content.SubmissionPoint;
import io.github.dostepnykampus.barrier.submission.content.json.EntrancePlaces;
import io.github.dostepnykampus.building.api.EntrancePlace;
import io.github.dostepnykampus.building.api.KnownBuildings;
import java.util.Optional;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * Miejsce zgłoszenia przy wejściu z wejść znanych modułowi budynków.
 *
 * <p>Wymagania: BR-8, BR-14</p>
 *
 * @since 0.1.0
 */
@Service
public final class BuildingEntrancePlaces implements EntrancePlaces {

    /**
     * Budynki i wejścia znane serwerowi.
     */
    private final KnownBuildings buildings;

    /**
     * Tworzy miejsca wejść z modułu budynków.
     *
     * @param buildings Budynki i wejścia znane serwerowi
     */
    public BuildingEntrancePlaces(final KnownBuildings buildings) {
        this.buildings = buildings;
    }

    @Override
    public Optional<SubmissionPlace> place(final String id, final @Nullable String symbol) {
        return this.buildings.entrance(id, symbol).map(BuildingEntrancePlaces::placeOf);
    }

    private static SubmissionPlace placeOf(final EntrancePlace entrance) {
        return new SubmissionPlace(
            new SubmissionPoint(entrance.longitude(), entrance.latitude()), null, false,
            entrance.buildingSymbol(),
            Optional.ofNullable(entrance.floorId())
                .map(id -> new SubmissionFloor(id, null, false))
                .orElse(null)
        );
    }
}
