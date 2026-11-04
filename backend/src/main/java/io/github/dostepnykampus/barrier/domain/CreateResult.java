/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.domain;

import java.util.Objects;

/**
 * Wynik utworzenia zgłoszenia (BR-9).
 *
 * @since 0.1.0
 */
public sealed interface CreateResult
    permits CreateResult.Created, CreateResult.Existing {

    /**
     * Zgłoszenie, którego dotyczy wynik.
     *
     * @return Zgłoszenie utworzone albo już istniejące
     */
    Submission submission();

    /**
     * Zgłoszenie utworzone tym wywołaniem.
     *
     * @param submission Utworzone zgłoszenie
     * @since 0.1.0
     */
    record Created(Submission submission) implements CreateResult {

        /**
         * Wynik z utworzonym zgłoszeniem; brak zgłoszenia kończy się wyjątkiem
         * NullPointerException.
         */
        public Created {
            Objects.requireNonNull(submission, "submission");
        }
    }

    /**
     * Zgłoszenie o tym identyfikatorze już istniało.
     *
     * @param submission Istniejące zgłoszenie
     * @since 0.1.0
     */
    record Existing(Submission submission) implements CreateResult {

        /**
         * Wynik z istniejącym zgłoszeniem; brak zgłoszenia kończy się wyjątkiem
         * NullPointerException.
         */
        public Existing {
            Objects.requireNonNull(submission, "submission");
        }
    }
}
