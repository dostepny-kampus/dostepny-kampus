/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.submission;

import java.util.Objects;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Decyzja Weryfikatora o zgłoszeniu, które wziął do weryfikacji.
 *
 * <p>Wymagania: US10, BR-3, BR-11, BR-16</p>
 *
 * @param id Identyfikator zgłoszenia
 * @param expectedVersion Wersja, którą ocenił Weryfikator
 * @param reviewer Identyfikator konta Weryfikatora
 * @param outcome Status po decyzji: ACCEPTED albo REJECTED
 * @param reason Powód odrzucenia albo null przy przyjęciu
 * @since 0.1.0
 */
public record ReviewDecision(
    UUID id, int expectedVersion, String reviewer, SubmissionStatus outcome,
    @Nullable String reason
) {

    /**
     * Decyzja bez braków; brak identyfikatora, konta albo wyniku kończy się wyjątkiem
     * NullPointerException.
     */
    public ReviewDecision {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(reviewer, "reviewer");
        Objects.requireNonNull(outcome, "outcome");
    }
}
