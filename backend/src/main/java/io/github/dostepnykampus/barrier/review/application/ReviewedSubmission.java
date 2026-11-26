/*
 * Dostępny Kampus
 * Author: Vladyslav Yevkov
 * Copyright (c) 2026 Vladyslav Yevkov, Myroslav Hryniuka, Yevhenii Lukianenko
 */
package io.github.dostepnykampus.barrier.review.application;

import io.github.dostepnykampus.barrier.submission.AuthorRole;
import io.github.dostepnykampus.barrier.submission.Submission;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Zgłoszenie w weryfikacji z autorem przedstawionym bez danych osobowych.
 *
 * <p>Wymagania: BR-5</p>
 *
 * @param submission Zgłoszenie
 * @param pseudonym Pseudonim autora albo {@code null}, gdy serwer nie zna konta autora
 * @param roles Role autora z chwili wysłania; pusta lista, gdy serwer ich nie zapisał
 * @since 0.1.0
 */
public record ReviewedSubmission(
    Submission submission, @Nullable String pseudonym, List<AuthorRole> roles
) {

    /**
     * Zgłoszenie z autorem i niemodyfikowalną kopią ról.
     */
    public ReviewedSubmission {
        roles = List.copyOf(roles);
    }
}
