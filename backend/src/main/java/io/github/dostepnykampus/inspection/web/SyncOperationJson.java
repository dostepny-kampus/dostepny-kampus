/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.domain.MarkerPlace;
import io.github.dostepnykampus.inspection.domain.NewMarker;
import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Jedna zmiana z telefonu w żądaniu, schemat SyncOperation w OpenAPI.
 *
 * @param clientOperationId Identyfikator operacji z telefonu albo null, gdy nie podano
 * @param type Rodzaj zmiany albo null, gdy nie podano
 * @param marker Znacznik przy rodzaju SET_INSPECTION_MARKER albo null
 * @param markerId Cofany znacznik przy rodzaju REVOKE_INSPECTION_MARKER albo null
 * @since 0.1.0
 */
record SyncOperationJson(
    @Nullable UUID clientOperationId, @Nullable String type, @Nullable Marker marker,
    @Nullable UUID markerId
) {

    /**
     * Identyfikator operacji z telefonu.
     *
     * @return Identyfikator z pola clientOperationId
     */
    UUID operation() {
        if (this.clientOperationId == null) {
            throw new InvalidInput("Brak identyfikatora operacji w polu clientOperationId");
        }
        return this.clientOperationId;
    }

    /**
     * Mówi, czy operacja stawia znacznik piętra obejrzanego.
     *
     * @return Prawda dla rodzaju SET_INSPECTION_MARKER
     */
    boolean placesMarker() {
        return "SET_INSPECTION_MARKER".equals(this.type);
    }

    /**
     * Mówi, czy operacja cofa znacznik piętra obejrzanego.
     *
     * @return Prawda dla rodzaju REVOKE_INSPECTION_MARKER
     */
    boolean revokesMarker() {
        return "REVOKE_INSPECTION_MARKER".equals(this.type);
    }

    /**
     * Znacznik do cofnięcia.
     *
     * @return Identyfikator z pola markerId
     */
    UUID revoked() {
        if (this.markerId == null) {
            throw new InvalidInput("Brak identyfikatora znacznika w polu markerId");
        }
        return this.markerId;
    }

    /**
     * Znacznik do zapisu.
     *
     * @param author Identyfikator konta z tokenu
     * @return Znacznik z pola marker
     */
    NewMarker toMarker(final String author) {
        if (this.marker == null) {
            throw new InvalidInput("Brak znacznika w polu marker");
        }
        return this.marker.toCommand(author);
    }

    /**
     * Sprawdza kształt operacji: identyfikator, znacznik przy jego postawieniu i identyfikator
     * znacznika przy cofnięciu.
     *
     * @param author Identyfikator konta z tokenu
     */
    void check(final String author) {
        this.operation();
        if (this.placesMarker()) {
            this.toMarker(author);
        }
        if (this.revokesMarker()) {
            this.revoked();
        }
    }

    /**
     * Znacznik piętra obejrzanego w żądaniu, schemat NewInspectionMarker w OpenAPI.
     *
     * @param id Identyfikator znacznika nadany w telefonie albo null, gdy nie podano
     * @param buildingSymbol Symbol budynku albo null, gdy nie podano
     * @param floorId Piętro obejrzane albo null w budynku podstawowym
     * @since 0.1.0
     */
    record Marker(
        @Nullable UUID id, @Nullable String buildingSymbol, @Nullable String floorId
    ) {

        /**
         * Przenosi znacznik z żądania do komendy domeny.
         *
         * @param author Identyfikator konta z tokenu
         * @return Znacznik do zapisu
         */
        NewMarker toCommand(final String author) {
            if (this.id == null || this.buildingSymbol == null || this.buildingSymbol.isBlank()) {
                throw new InvalidInput("Znacznik wymaga pól marker.id i marker.buildingSymbol");
            }
            MarkerPlace place = new MarkerPlace.Building(this.buildingSymbol);
            if (this.floorId != null) {
                place = new MarkerPlace.Floor(this.buildingSymbol, this.floorId);
            }
            return new NewMarker(this.id, place, author);
        }
    }
}
