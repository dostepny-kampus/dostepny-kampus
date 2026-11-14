/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.barrier.submission.content.SubmissionFloor;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Piętro zgłoszenia, schemat SubmissionFloor w OpenAPI.
 *
 * @param floorId Identyfikator piętra albo null
 * @param number Numer piętra albo null
 * @param mezzanine Półpiętro albo null, gdy nie podano lub fałsz
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SubmissionFloorJson(
    @Nullable String floorId, @Nullable Integer number, @Nullable Boolean mezzanine
) {

    /**
     * Zapisuje piętro zgłoszenia; fałszywe półpiętro nie trafia do JSON.
     *
     * @param floor Piętro zgłoszenia
     */
    SubmissionFloorJson(final SubmissionFloor floor) {
        this(
            floor.floorId(), floor.number(),
            Optional.of(floor.mezzanine()).filter(Boolean::booleanValue).orElse(null)
        );
    }

    /**
     * Przenosi piętro z JSON do domeny.
     *
     * @return Piętro zgłoszenia
     */
    SubmissionFloor toFloor() {
        return new SubmissionFloor(this.floorId, this.number, Boolean.TRUE.equals(this.mezzanine));
    }
}
