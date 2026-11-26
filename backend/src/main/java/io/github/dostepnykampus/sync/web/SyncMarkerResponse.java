/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.github.dostepnykampus.sync.application.PulledMarker;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Znacznik piętra według schematu InspectionMarker.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3</p>
 *
 * @param id Identyfikator znacznika
 * @param buildingSymbol Symbol budynku
 * @param floorId Identyfikator piętra, brak przy znaczniku całego budynku
 * @param createdAt Chwila przyjęcia znacznika przez serwer
 * @param revoked Czy znacznik cofnięto
 * @param mine Czy znacznik postawiła osoba pobierająca zmiany
 * @since 0.1.0
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
record SyncMarkerResponse(
    UUID id, String buildingSymbol, @Nullable String floorId, Instant createdAt,
    boolean revoked, boolean mine
) {

    static SyncMarkerResponse from(final PulledMarker marker) {
        return new SyncMarkerResponse(
            marker.id(), marker.buildingSymbol(), marker.floorId(), marker.created(),
            marker.revoked(), marker.mine()
        );
    }
}
