/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.ProgressMarker;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Aktywny znacznik w postępie inwentaryzacji. Autor jest widoczny pod pseudonimem.
 *
 * @param id Identyfikator znacznika
 * @param buildingSymbol Symbol budynku
 * @param floorId Identyfikator piętra, brak przy znaczniku całego budynku
 * @param createdAt Chwila przyjęcia znacznika przez serwer
 * @param revoked Czy znacznik cofnięto, zawsze fałsz dla aktywnych znaczników
 * @param mine Czy znacznik należy do czytającego postęp
 * @param authorPseudonym Pseudonim autora
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record ProgressMarkerResponse(
    UUID id,
    String buildingSymbol,
    @Nullable String floorId,
    Instant createdAt,
    boolean revoked,
    boolean mine,
    String authorPseudonym
) {

    static ProgressMarkerResponse from(final ProgressMarker marker) {
        final MarkerPlace place = marker.place();
        final String level;
        if (place instanceof MarkerPlace.Floor floor) {
            level = floor.floor();
        } else {
            level = null;
        }
        return new ProgressMarkerResponse(
            marker.id(), place.building(), level,
            marker.created(), false, marker.mine(), marker.pseudonym()
        );
    }
}
