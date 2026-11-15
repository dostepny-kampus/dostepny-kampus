/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.Objects;
import java.util.UUID;

/**
 * Decyzja Weryfikatora o zgłoszeniu, które wziął do weryfikacji: przyjęcie albo
 * odrzucenie z niepustym powodem.
 *
 * <p>Wymagania: US10, BR-3, BR-11, BR-16</p>
 *
 * @since 0.1.0
 */
public sealed interface ReviewDecision
    permits ReviewDecision.Accepted, ReviewDecision.Rejected {

    /**
     * Identyfikator zgłoszenia.
     *
     * @return Identyfikator zgłoszenia, którego dotyczy decyzja
     */
    UUID id();

    /**
     * Wersja, którą ocenił Weryfikator.
     *
     * @return Oczekiwana wersja zgłoszenia
     */
    int expectedVersion();

    /**
     * Konto Weryfikatora.
     *
     * @return Identyfikator konta Weryfikatora
     */
    String reviewer();

    /**
     * Status po decyzji.
     *
     * @return ACCEPTED albo REJECTED
     */
    SubmissionStatus outcome();

    /**
     * Przyjęcie zgłoszenia.
     *
     * @param id Identyfikator zgłoszenia
     * @param expectedVersion Wersja, którą ocenił Weryfikator
     * @param reviewer Identyfikator konta Weryfikatora
     * @since 0.1.0
     */
    record Accepted(UUID id, int expectedVersion, String reviewer) implements ReviewDecision {

        /**
         * Przyjęcie bez braków; brak identyfikatora albo konta kończy się wyjątkiem
         * NullPointerException.
         *
         * @param id Identyfikator zgłoszenia
         * @param expectedVersion Wersja, którą ocenił Weryfikator
         * @param reviewer Identyfikator konta Weryfikatora
         */
        public Accepted {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(reviewer, "reviewer");
        }

        @Override
        public SubmissionStatus outcome() {
            return SubmissionStatus.ACCEPTED;
        }
    }

    /**
     * Odrzucenie zgłoszenia z powodem.
     *
     * @param id Identyfikator zgłoszenia
     * @param expectedVersion Wersja, którą ocenił Weryfikator
     * @param reviewer Identyfikator konta Weryfikatora
     * @param reason Niepusty powód odrzucenia
     * @since 0.1.0
     */
    record Rejected(
        UUID id, int expectedVersion, String reviewer, String reason
    ) implements ReviewDecision {

        /**
         * Odrzucenie bez braków; brak identyfikatora, konta albo powodu kończy się wyjątkiem
         * NullPointerException, a pusty powód wyjątkiem IllegalArgumentException.
         *
         * @param id Identyfikator zgłoszenia
         * @param expectedVersion Wersja, którą ocenił Weryfikator
         * @param reviewer Identyfikator konta Weryfikatora
         * @param reason Niepusty powód odrzucenia
         */
        public Rejected {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(reviewer, "reviewer");
            if (Objects.requireNonNull(reason, "reason").isBlank()) {
                throw new IllegalArgumentException("Odrzucenie wymaga niepustego powodu");
            }
        }

        @Override
        public SubmissionStatus outcome() {
            return SubmissionStatus.REJECTED;
        }
    }
}
