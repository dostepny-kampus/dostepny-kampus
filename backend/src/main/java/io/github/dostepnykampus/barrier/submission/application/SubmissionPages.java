/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Stream;

/**
 * Zgłoszenia ze wszystkich stron listy jako leniwy strumień: następną stronę czyta dopiero,
 * gdy odbiorca przeczyta poprzednią, więc w pamięci jest najwyżej jedna strona.
 *
 * <p>Wymagania: NFR-34</p>
 *
 * @since 0.1.0
 */
final class SubmissionPages {

    /**
     * Strona po kursorze; kursor {@link SubmissionFinder.Page#START} daje pierwszą stronę.
     */
    private final Function<UUID, SubmissionFinder.Page> pages;

    /**
     * Tworzy strumień stron.
     *
     * @param pages Strona po kursorze; kursor początku daje pierwszą stronę
     */
    SubmissionPages(final Function<UUID, SubmissionFinder.Page> pages) {
        this.pages = pages;
    }

    /**
     * Zgłoszenia kolejnych stron.
     *
     * @return Leniwy strumień zgłoszeń wszystkich stron
     */
    Stream<Submission> stream() {
        return Stream.iterate(
            Optional.of(this.pages.apply(SubmissionFinder.Page.START)), Optional::isPresent,
            page -> page.filter(SubmissionFinder.Page::more)
                .map(item -> this.pages.apply(item.last()))
        ).flatMap(page -> page.orElseThrow().items().stream());
    }
}
