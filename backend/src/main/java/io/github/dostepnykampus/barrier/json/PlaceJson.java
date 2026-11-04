/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionPlace;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Miejsce zgłoszenia w JSON: pola SubmissionContent związane z położeniem.
 *
 * @param location Punkt GeoJSON
 * @param accuracyMeters Dokładność pozycji w metrach albo null
 * @param manualCorrection Korekta ręczna albo null, gdy nie podano lub fałsz
 * @param buildingSymbol Symbol budynku albo null
 * @param floor Piętro albo null
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record PlaceJson(
    @Nullable GeoPointJson location, @Nullable Double accuracyMeters,
    @Nullable Boolean manualCorrection, @Nullable String buildingSymbol,
    @Nullable SubmissionFloorJson floor
) {

    /**
     * Zapisuje miejsce zgłoszenia; fałszywa korekta nie trafia do JSON.
     *
     * @param place Miejsce zgłoszenia
     */
    PlaceJson(final SubmissionPlace place) {
        this(
            new GeoPointJson(place.point()), place.accuracyMeters(),
            Optional.of(place.manualCorrection()).filter(Boolean::booleanValue).orElse(null),
            place.buildingSymbol(),
            Optional.ofNullable(place.floor()).map(SubmissionFloorJson::new).orElse(null)
        );
    }

    /**
     * Przenosi miejsce z JSON do domeny.
     *
     * @return Miejsce zgłoszenia
     */
    SubmissionPlace toPlace() {
        if (this.location == null) {
            throw new InvalidSubmission("Zgłoszenie wymaga pola location");
        }
        return new SubmissionPlace(
            this.location.toPoint(), this.accuracyMeters,
            Boolean.TRUE.equals(this.manualCorrection), this.buildingSymbol,
            Optional.ofNullable(this.floor).map(SubmissionFloorJson::toFloor).orElse(null)
        );
    }
}
