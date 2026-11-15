/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.application.MarkerService;
import java.security.Principal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * Wysyłka zmian z telefonu: serwer zapisuje znaczniki piętra obejrzanego, a pozostałe
 * rodzaje zmian odrzuca jako jeszcze nieobsługiwane.
 *
 * <p>TODO(#2139): przyjmować operacje CREATE_SUBMISSION i UPDATE_SUBMISSION.</p>
 *
 * <p>TODO(#2141): przyjmować operację REVOKE_INSPECTION_MARKER.</p>
 *
 * <p>Wymagania: US15.1, US15.3, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SyncPushController {

    /**
     * Zapis znaczników piętra w transakcji.
     */
    private final MarkerService service;

    SyncPushController(final MarkerService service) {
        this.service = service;
    }

    /**
     * Zapisuje znaczniki z paczki w jednej transakcji; wynik każdej operacji jest osobny, a
     * znacznik o zapisanym już identyfikatorze nie powstaje drugi raz. Błąd w ciele którejkolwiek
     * operacji daje 400 przed zapisem.
     *
     * <p>Wymagania: US15.1, US15.3, BR-9, BR-20</p>
     *
     * @param body Paczka według schematu z żądania
     * @param account Zalogowane konto; jego nazwa to identyfikator z tokenu
     * @return Wyniki operacji w kolejności żądania
     */
    @PostMapping("/sync/push")
    SyncPushResponse push(@RequestBody final SyncPushRequest body, final Principal account) {
        final String author = account.getName();
        final List<SyncOperationJson> operations = body.checked();
        operations.forEach(operation -> operation.check(author));
        final Set<UUID> unknown = this.service.place(
            operations.stream().filter(SyncOperationJson::placesMarker)
                .map(operation -> operation.toMarker(author)).toList()
        );
        return new SyncPushResponse(
            operations.stream()
                .map(operation -> SyncPushController.result(operation, author, unknown)).toList()
        );
    }

    private static SyncResultResponse result(
        final SyncOperationJson operation, final String author, final Set<UUID> unknown
    ) {
        final UUID id = operation.operation();
        SyncResultResponse result = SyncResultResponse.rejected(
            id, HttpStatus.NOT_IMPLEMENTED, "Serwer nie przyjmuje jeszcze tej zmiany"
        );
        if (operation.placesMarker() && unknown.contains(operation.toMarker(author).id())) {
            result = SyncResultResponse.rejected(id, HttpStatus.BAD_REQUEST, "Nieznany budynek");
        } else if (operation.placesMarker()) {
            result = SyncResultResponse.applied(id);
        }
        return result;
    }
}
