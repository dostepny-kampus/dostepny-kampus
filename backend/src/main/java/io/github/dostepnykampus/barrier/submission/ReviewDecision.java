/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import io.github.dostepnykampus.barrier.submission.content.BarrierType;
import io.github.dostepnykampus.barrier.submission.content.InvalidSubmission;
import io.github.dostepnykampus.barrier.submission.content.SubmissionKind;
import java.util.Objects;
import java.util.UUID;

/**
 * Decyzja Weryfikatora o zgłoszeniu, które wziął do weryfikacji: przyjęcie albo
 * odrzucenie z niepustym powodem. Przyjęcie zgłoszenia szybkiego niesie typ bariery
 * i uciążliwość ustalone przez Weryfikatora, przyjęcie innych zgłoszeń ich nie niesie.
 *
 * <p>Wymagania: US10, US10.3, BR-3, BR-11, BR-14, BR-16</p>
 *
 * @since 0.1.0
 */
public sealed interface ReviewDecision
    permits ReviewDecision.Accepted, ReviewDecision.Classified, ReviewDecision.Rejected {

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
     * Sprawdza, czy decyzja pasuje do rodzaju zgłoszenia: przyjęcie zgłoszenia szybkiego
     * wymaga typu i uciążliwości, a przyjęcie innego rodzaju ich nie przyjmuje; niezgodność
     * kończy się wyjątkiem {@link InvalidSubmission}.
     *
     * <p>Wymagania: US10.3, BR-14</p>
     *
     * @param kind Rodzaj zgłoszenia, którego dotyczy decyzja
     */
    void admit(SubmissionKind kind);

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

        @Override
        public void admit(final SubmissionKind kind) {
            if (kind == SubmissionKind.QUICK) {
                throw new InvalidSubmission(
                    "Przyjęcie zgłoszenia szybkiego wymaga pól barrierType i severity (BR-14)"
                );
            }
        }
    }

    /**
     * Przyjęcie zgłoszenia szybkiego z typem bariery i uciążliwością.
     *
     * @param id Identyfikator zgłoszenia
     * @param expectedVersion Wersja, którą ocenił Weryfikator
     * @param reviewer Identyfikator konta Weryfikatora
     * @param type Typ bariery ustalony przez Weryfikatora
     * @param severity Uciążliwość od 1 do 3 ustalona przez Weryfikatora
     * @since 0.1.0
     */
    record Classified(
        UUID id, int expectedVersion, String reviewer, BarrierType type, int severity
    ) implements ReviewDecision {

        /**
         * Przyjęcie z typem i uciążliwością; brak identyfikatora, konta albo typu kończy się
         * wyjątkiem NullPointerException, a uciążliwość spoza zakresu 1–3 wyjątkiem
         * {@link InvalidSubmission}.
         *
         * @param id Identyfikator zgłoszenia
         * @param expectedVersion Wersja, którą ocenił Weryfikator
         * @param reviewer Identyfikator konta Weryfikatora
         * @param type Typ bariery ustalony przez Weryfikatora
         * @param severity Uciążliwość od 1 do 3 ustalona przez Weryfikatora
         */
        public Classified {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(reviewer, "reviewer");
            Objects.requireNonNull(type, "type");
            if (severity < 1 || severity > 3) {
                throw new InvalidSubmission("Uciążliwość spoza zakresu 1–3");
            }
        }

        @Override
        public SubmissionStatus outcome() {
            return SubmissionStatus.ACCEPTED;
        }

        @Override
        public void admit(final SubmissionKind kind) {
            if (kind != SubmissionKind.QUICK) {
                throw new InvalidSubmission(
                    "Typ bariery i uciążliwość ustala się tylko przy zgłoszeniu szybkim (BR-14)"
                );
            }
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

        @Override
        public void admit(final SubmissionKind kind) {
            Objects.requireNonNull(kind, "kind");
        }
    }
}
