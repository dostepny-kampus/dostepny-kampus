/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission.application;

import io.github.dostepnykampus.barrier.submission.Submission;
import io.github.dostepnykampus.barrier.submission.SubmissionFinder;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Kolejka weryfikacji dla warstwy web: kontroler nie sięga do portu
 * {@link SubmissionFinder}, tylko do tej usługi.
 *
 * <p>Wymagania: US10.4, US10.5, US10.7, BR-4, BR-11</p>
 *
 * @since 0.1.0
 */
@Service
public final class ReviewQueue {

    /** Port odczytu zgłoszeń. */
    private final SubmissionFinder finder;

    /**
     * Ctor.
     *
     * @param finder Port odczytu zgłoszeń
     */
    public ReviewQueue(final SubmissionFinder finder) {
        this.finder = finder;
    }

    /**
     * Wyszukuje zgłoszenia w kolejce weryfikacji dla danego konta Weryfikatora,
     * bez kontroli kompletu zdjęć w magazynie.
     *
     * @param reviewer Identyfikator konta Weryfikatora
     * @return Zgłoszenia w kolejce od najdawniej wysłanego
     */
    public List<Submission> forReviewer(final String reviewer) {
        return this.finder.reviewQueue(reviewer);
    }
}
