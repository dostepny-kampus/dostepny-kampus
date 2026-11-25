/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.inspection.web;

import io.github.dostepnykampus.inspection.application.MarkerService;
import io.github.dostepnykampus.inspection.domain.SyncedMarker;
import io.github.dostepnykampus.shared.error.InvalidInput;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pobranie zmian do telefonu: znaczniki pięter zmienione od kursora, także cofnięte.
 *
 * <p>TODO(#3851): zgłoszenia w pobraniu zmian.</p>
 *
 * <p>TODO(#3852): wejścia ze zmienionym oznaczeniem w pobraniu zmian.</p>
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, BR-20</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SyncPullController {

    /** Odczyt znaczników piętra. */
    private final MarkerService service;

    SyncPullController(final MarkerService service) {
        this.service = service;
    }

    /**
     * Zmiany od kursora {@code since} włącznie; brak kursora daje wszystko. Kursor odpowiedzi to
     * chwila najpóźniejszej zwróconej zmiany, a bez zmian kursor z żądania. Znaczniki dostaje
     * tylko rola Zbierającego, inne role pustą listę. Kursor, który nie jest chwilą ISO 8601,
     * daje 400.
     *
     * @param since Kursor z poprzedniego pobrania
     * @param account Zalogowane konto; jego nazwa to identyfikator z tokenu
     * @param request Bieżące żądanie HTTP; z niego pochodzi rola konta
     * @return Zmiany i kursor następnego pobrania
     */
    @GetMapping("/sync/pull")
    SyncPullResponse pull(
        @RequestParam(required = false) final @Nullable String since, final Principal account,
        final HttpServletRequest request
    ) {
        final Instant from = SyncPullController.cursor(since);
        List<SyncedMarker> markers = List.of();
        if (request.isUserInRole("COLLECTOR")) {
            markers = this.service.changedSince(from, account.getName());
        }
        return new SyncPullResponse(
            markers.stream().map(SyncedMarker::changed).max(Comparator.naturalOrder())
                .orElse(from).toString(),
            List.of(), List.of(), markers.stream().map(SyncMarkerResponse::from).toList()
        );
    }

    private static Instant cursor(final @Nullable String since) {
        Instant result = Instant.EPOCH;
        if (since != null) {
            try {
                result = Instant.parse(since);
            } catch (final DateTimeParseException ex) {
                throw new InvalidInput("Parametr since nie jest kursorem pobrania", ex);
            }
        }
        return result;
    }
}
