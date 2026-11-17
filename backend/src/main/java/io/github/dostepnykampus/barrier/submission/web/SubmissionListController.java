/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.barrier.submission.application.SubmissionReading;
import io.github.dostepnykampus.shared.error.MissingLogin;
import java.security.Principal;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Lista zgłoszeń barier stronami: zgłoszenia przyjęte według filtrów
 * albo własne zgłoszenia zalogowanej osoby.
 *
 * <p>Wymagania: US1.3, US2.2, BR-6, US8.1, NFR-34</p>
 *
 * @since 0.1.0
 */
@RestController
public final class SubmissionListController {

    /**
     * Odczyt zgłoszeń.
     */
    private final SubmissionReading finder;

    SubmissionListController(final SubmissionReading finder) {
        this.finder = finder;
    }

    /**
     * Zwraca stronę zgłoszeń przyjętych według filtrów, a z mine=true własnych zgłoszeń
     * zalogowanej osoby w każdym statusie. Dalszy ciąg wskazuje nagłówek Link z rel="next".
     *
     * <p>Wymagania: US1.3, US2.2, BR-6, US8.1, NFR-34</p>
     *
     * @param filter Filtry z parametrów zapytania
     * @param window Rozmiar strony i kursor
     * @param account Zalogowane konto albo null bez logowania
     * @return Zgłoszenia strony w kolejności identyfikatorów
     */
    @GetMapping("/submissions")
    ResponseEntity<List<SubmissionResponse>> listSubmissions(
        final SubmissionFilter filter, final SubmissionWindow window,
        @Nullable final Principal account
    ) {
        @Nullable String author = null;
        if (filter.own()) {
            author = SubmissionListController.name(account);
        }
        return new SubmissionPageResponse(
            this.finder.page(filter.toQuery(), author, window.window())
        ).entity();
    }

    private static String name(@Nullable final Principal account) {
        if (account == null) {
            throw new MissingLogin("Własne zgłoszenia wymagają logowania");
        }
        return account.getName();
    }
}
