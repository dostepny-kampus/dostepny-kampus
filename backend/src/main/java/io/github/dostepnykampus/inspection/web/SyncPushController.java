/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.application.MarkerService;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import tools.jackson.databind.json.JsonMapper;

/**
 * Wysyłka zmian z telefonu: serwer zapisuje i cofa znaczniki piętra obejrzanego, a pozostałe
 * rodzaje zmian odrzuca jako jeszcze nieobsługiwane.
 *
 * <p>TODO(#2139): przyjmować operacje CREATE_SUBMISSION i UPDATE_SUBMISSION.</p>
 *
 * <p>Wymagania: US15.1, US15.2, US15.3, BR-9, BR-20</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SyncPushController {

    /**
     * Zapis znaczników piętra w transakcji.
     */
    private final MarkerService service;

    /**
     * Odczyt każdej operacji osobno.
     */
    private final SyncOperationReader reader;

    SyncPushController(final MarkerService service, final JsonMapper mapper) {
        this(service, new SyncOperationReader(mapper));
    }

    private SyncPushController(final MarkerService service, final SyncOperationReader reader) {
        this.service = service;
        this.reader = reader;
    }

    /**
     * Zapisuje znaczniki z paczki w jednej transakcji, raz na identyfikator; wynik każdej
     * operacji jest osobny, a błąd danych jednej daje jej REJECTED 400. Cofnięcia idą po
     * zapisach: własny znacznik, także powtórnie, jest cofnięty, cudzy albo nieistniejący
     * odrzucony. Znacznik od konta bez roli Zbierającego dostaje 403. 400 dla całej paczki
     * daje tylko zła liczba operacji albo brak clientOperationId.
     *
     * <p>Wymagania: US15.1, US15.3, BR-9, BR-20, US9.2</p>
     *
     * @param body Paczka według schematu z żądania
     * @param account Zalogowane konto; jego nazwa to identyfikator z tokenu
     * @param request Bieżące żądanie HTTP; z niego pochodzi rola konta
     * @return Wyniki operacji w kolejności żądania
     */
    @PostMapping("/sync/push")
    SyncPushResponse push(
        @RequestBody final SyncPushRequest body, final Principal account,
        final HttpServletRequest request
    ) {
        final String author = account.getName();
        final boolean collector = request.isUserInRole("COLLECTOR");
        final List<ReadOperation> read = body.checked().stream()
            .map(node -> this.reader.read(node, author)).toList();
        final List<SyncOperationJson> operations = read.stream()
            .flatMap(SyncPushController::valid).toList();
        final Set<UUID> unknown = this.service.place(
            operations.stream().filter(operation -> collector && operation.placesMarker())
                .map(operation -> operation.toMarker(author)).toList()
        );
        final Set<UUID> missing = this.service.revoke(
            operations.stream().filter(SyncOperationJson::revokesMarker)
                .map(SyncOperationJson::revoked).toList(),
            author
        );
        return new SyncPushResponse(
            read.stream().map(
                operation -> switch (operation) {
                    case ReadOperation.Broken broken -> SyncResultResponse.rejected(
                        broken.id(), HttpStatus.BAD_REQUEST, broken.detail()
                    );
                    case ReadOperation.Valid valid -> SyncPushController.allowed(
                        valid.operation(), collector
                    ).orElseGet(
                        () -> SyncPushController.result(valid.operation(), author, unknown, missing)
                    );
                    case null -> throw new IllegalStateException("Brak odczytanej operacji");
                }
            ).toList()
        );
    }

    private static Stream<SyncOperationJson> valid(final ReadOperation operation) {
        Stream<SyncOperationJson> result = Stream.empty();
        if (operation instanceof ReadOperation.Valid valid) {
            result = Stream.of(valid.operation());
        }
        return result;
    }

    private static Optional<SyncResultResponse> allowed(
        final SyncOperationJson operation, final boolean collector
    ) {
        Optional<SyncResultResponse> result = Optional.empty();
        if (operation.placesMarker() && !collector) {
            result = Optional.of(
                SyncResultResponse.rejected(
                    operation.operation(), HttpStatus.FORBIDDEN,
                    "Znacznik obejrzanego wymaga roli Zbierającego"
                )
            );
        }
        return result;
    }

    private static SyncResultResponse result(
        final SyncOperationJson operation, final String author, final Set<UUID> unknown,
        final Set<UUID> missing
    ) {
        final UUID id = operation.operation();
        SyncResultResponse result = SyncResultResponse.rejected(
            id, HttpStatus.NOT_IMPLEMENTED, "Serwer nie przyjmuje jeszcze tej zmiany"
        );
        if (operation.placesMarker() && unknown.contains(operation.toMarker(author).id())) {
            result = SyncResultResponse.rejected(
                id, HttpStatus.BAD_REQUEST, "Nieznany budynek albo piętro"
            );
        } else if (operation.placesMarker()) {
            result = SyncResultResponse.applied(id);
        } else if (operation.revokesMarker() && missing.contains(operation.revoked())) {
            result = SyncResultResponse.rejected(
                id, HttpStatus.NOT_FOUND, "Nie ma Twojego znacznika o tym identyfikatorze"
            );
        } else if (operation.revokesMarker()) {
            result = SyncResultResponse.applied(id);
        }
        return result;
    }
}
