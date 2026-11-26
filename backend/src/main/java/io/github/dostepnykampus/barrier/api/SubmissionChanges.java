/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.api;

import java.time.Instant;
import java.util.List;

/**
 * Zgłoszenia zmienione od kursora do pobrania przez telefon.
 *
 * <p>Wymagania: US8.1, US1.3, US10.6</p>
 *
 * @since 0.1.0
 */
@FunctionalInterface
public interface SubmissionChanges {

    /**
     * Własne zgłoszenia konta w każdym statusie oraz cudze przyjęte i archiwalne, zmienione od
     * {@code since} włącznie, od najdawniej zmienionego.
     *
     * @param since Chwila, od której liczą się zmiany
     * @param reader Identyfikator konta z tokenu
     * @return Zgłoszenia z chwilą zmiany
     */
    List<ChangedSubmission> changedSince(Instant since, String reader);
}
