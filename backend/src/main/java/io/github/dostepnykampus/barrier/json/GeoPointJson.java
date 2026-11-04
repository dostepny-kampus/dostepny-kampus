/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.barrier.domain.InvalidSubmission;
import io.github.dostepnykampus.barrier.domain.SubmissionPoint;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Punkt GeoJSON, schemat GeoPoint w OpenAPI.
 *
 * @param type Rodzaj geometrii, zawsze Point
 * @param coordinates Długość i szerokość geograficzna, w tej kolejności
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record GeoPointJson(@Nullable String type, @Nullable List<Double> coordinates) {

    /** Rodzaj geometrii punktu. */
    private static final String POINT = "Point";

    /**
     * Zapisuje punkt zgłoszenia jako GeoJSON.
     *
     * @param point Punkt zgłoszenia
     */
    GeoPointJson(final SubmissionPoint point) {
        this(GeoPointJson.POINT, List.of(point.longitude(), point.latitude()));
    }

    /**
     * Przenosi punkt z JSON do domeny.
     *
     * @return Punkt zgłoszenia
     * @throws InvalidSubmission Gdy rodzaj nie jest Point albo współrzędnych
     *  nie ma dokładnie dwóch
     */
    SubmissionPoint toPoint() throws InvalidSubmission {
        if (!GeoPointJson.POINT.equals(this.type)) {
            throw new InvalidSubmission("Miejsce zgłoszenia musi mieć type Point");
        }
        final List<Double> pair = Optional.ofNullable(this.coordinates).orElse(List.of());
        if (pair.size() != 2) {
            throw new InvalidSubmission("Miejsce zgłoszenia wymaga dokładnie dwóch współrzędnych");
        }
        return new SubmissionPoint(pair.get(0), pair.get(1));
    }
}
