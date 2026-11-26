/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.application;

import io.github.dostepnykampus.barrier.submission.Submission;

/**
 * Stan zgłoszenia nie pozwala na wzięcie albo decyzję: brak zdjęcia w magazynie albo
 * nieaktualna zmiana oznaczenia wejścia. Niesie zgłoszenie sprzed operacji.
 *
 * <p>Wymagania: BR-7, BR-23, BR-24</p>
 *
 * @since 0.1.0
 */
public final class ReviewConflict extends RuntimeException {

    /** Identyfikator postaci serializowanej wyjątku. */
    private static final long serialVersionUID = 1L;

    /** Zgłoszenie sprzed operacji. */
    private final transient Submission state;

    /**
     * Konflikt z opisem dla klienta.
     *
     * @param detail Opis konfliktu dla klienta
     * @param state Zgłoszenie sprzed operacji
     */
    ReviewConflict(final String detail, final Submission state) {
        super(detail);
        this.state = state;
    }

    /**
     * Zgłoszenie sprzed operacji.
     *
     * @return Bieżące zgłoszenie na serwerze
     */
    public Submission current() {
        return this.state;
    }
}
