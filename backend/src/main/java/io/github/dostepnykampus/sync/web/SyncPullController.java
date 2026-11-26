/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.sync.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import io.github.dostepnykampus.sync.application.ChangeFeed;
import io.github.dostepnykampus.sync.application.PulledChanges;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Pobranie zmian do telefonu: zgłoszenia i znaczniki pięter zmienione od kursora.
 *
 * <p>Wymagania: US15.1, US15.2, US11.3, US8.1, US1.3, US10.6, BR-20</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SyncPullController {

    /** Zmiany od kursora z modułów właścicieli danych. */
    private final ChangeFeed feed;

    SyncPullController(final ChangeFeed feed) {
        this.feed = feed;
    }

    /**
     * Zmiany od kursora {@code since} włącznie; brak kursora daje wszystko. Kursor odpowiedzi to
     * chwila najpóźniejszej zwróconej zmiany, a bez zmian kursor z żądania. Zgłoszenia: własne
     * w każdym statusie oraz cudze przyjęte i archiwalne. Znaczniki dostaje tylko rola
     * Zbierającego, inne role pustą listę. Kursor, który nie jest chwilą ISO 8601,
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
        final PulledChanges changes = this.feed.pull(
            SyncPullController.cursor(since), account.getName(),
            request.isUserInRole("COLLECTOR")
        );
        return new SyncPullResponse(
            changes.cursor().toString(), changes.submissions(), List.of(),
            changes.markers().stream().map(SyncMarkerResponse::from).toList()
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
