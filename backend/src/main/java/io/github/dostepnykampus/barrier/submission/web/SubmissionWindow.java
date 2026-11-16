/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.web;

import io.github.dostepnykampus.shared.error.InvalidInput;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Strona listy zgłoszeń z parametrów {@code limit} i {@code after}.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @param after Kursor: identyfikator ostatniego zgłoszenia poprzedniej strony
 * @param limit Najwięcej zgłoszeń na stronie, od 1 do 500
 * @since 0.1.0
 */
record SubmissionWindow(@Nullable String after, @Nullable String limit) {

    /**
     * Największa strona i rozmiar strony bez parametru limit.
     */
    private static final int LARGEST = 500;

    /**
     * Rozmiar strony; limit spoza 1–500 kończy się wyjątkiem {@link InvalidInput}.
     *
     * @return Najwięcej zgłoszeń na stronie
     */
    int size() {
        int size = SubmissionWindow.LARGEST;
        if (this.limit != null) {
            try {
                size = Integer.parseInt(this.limit);
            } catch (final NumberFormatException ex) {
                throw new InvalidInput("Parametr limit przyjmuje liczbę od 1 do 500", ex);
            }
        }
        if (size < 1 || size > SubmissionWindow.LARGEST) {
            throw new InvalidInput("Parametr limit przyjmuje liczbę od 1 do 500");
        }
        return size;
    }

    /**
     * Kursor strony; zły identyfikator kończy się wyjątkiem {@link InvalidInput}.
     *
     * @return Identyfikator, po którym zaczyna się strona; brak dla pierwszej strony
     */
    @Nullable UUID cursor() {
        @Nullable UUID cursor = null;
        if (this.after != null) {
            try {
                cursor = UUID.fromString(this.after);
            } catch (final IllegalArgumentException ex) {
                throw new InvalidInput("Parametr after nie jest kursorem strony", ex);
            }
        }
        return cursor;
    }
}
